package com.attri.systemdesign.lld.ledger;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.ReentrantLock;

/**
 * In-memory reference implementation of an append-only, double-entry ledger.
 * In production, the critical section maps to a database transaction that locks
 * both account rows and persists the transaction, entries, balance projection,
 * and idempotency record atomically.
 */
public final class LedgerService {
    private static final int IDEMPOTENCY_LOCK_STRIPES = 256;

    private final Map<AccountId, Account> accounts = new ConcurrentHashMap<>();
    private final Map<String, LedgerTransaction> transactionsByKey = new ConcurrentHashMap<>();
    private final Map<UUID, LedgerTransaction> transactionsById = new ConcurrentHashMap<>();
    private final Map<UUID, UUID> reversalByOriginalId = new ConcurrentHashMap<>();
    private final ReentrantLock[] idempotencyLocks = new ReentrantLock[IDEMPOTENCY_LOCK_STRIPES];
    private final AtomicLong sequence = new AtomicLong();

    public LedgerService() {
        for (int i = 0; i < idempotencyLocks.length; i++) {
            idempotencyLocks[i] = new ReentrantLock();
        }
    }

    public void createAccount(AccountId accountId) {
        if (accounts.putIfAbsent(Objects.requireNonNull(accountId, "accountId must not be null"), new Account()) != null) {
            throw new IllegalArgumentException("account already exists: " + accountId.value());
        }
    }

    public LedgerTransaction recordTransaction(String idempotencyKey, AccountId fromAccount,
                                               AccountId toAccount, BigDecimal amount, TransactionType type) {
        return record(idempotencyKey, fromAccount, toAccount, amount, type, null);
    }

    /** Reverses a transaction by posting an equal and opposite, linked transaction. */
    public LedgerTransaction reverseTransaction(String idempotencyKey, UUID originalTransactionId,
                                                TransactionType reversalType) {
        LedgerTransaction original = requireTransaction(originalTransactionId);
        return record(idempotencyKey, original.toAccount(), original.fromAccount(), original.amount(),
                reversalType, original.id());
    }

    public BigDecimal getBalance(AccountId accountId) {
        Account account = requireAccount(accountId);
        account.lock.lock();
        try {
            return account.balance;
        } finally {
            account.lock.unlock();
        }
    }

    public AccountHistoryPage getTransactionHistory(AccountId accountId, int pageNumber, int pageSize) {
        if (pageNumber < 0 || pageSize <= 0) {
            throw new IllegalArgumentException("pageNumber must be >= 0 and pageSize must be > 0");
        }
        Account account = requireAccount(accountId);
        account.lock.lock();
        try {
            long total = account.entries.size();
            int start = Math.toIntExact(Math.min((long) pageNumber * pageSize, total));
            int end = Math.min(start + pageSize, account.entries.size());
            return new AccountHistoryPage(account.entries.subList(start, end), pageNumber, pageSize, total);
        } finally {
            account.lock.unlock();
        }
    }

    /** Rebuilds the account balance from immutable entries, optionally at a historical instant. */
    public BigDecimal reconstructBalance(AccountId accountId, Instant asOfInclusive) {
        Objects.requireNonNull(asOfInclusive, "asOfInclusive must not be null");
        Account account = requireAccount(accountId);
        account.lock.lock();
        try {
            return account.entries.stream()
                    .filter(entry -> !entry.recordedAt().isAfter(asOfInclusive))
                    .map(LedgerEntry::signedAmount)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
        } finally {
            account.lock.unlock();
        }
    }

    /** Rebuilds a balance through an exact point in the globally ordered entry log. */
    public BigDecimal reconstructBalance(AccountId accountId, long throughSequenceInclusive) {
        if (throughSequenceInclusive < 0) {
            throw new IllegalArgumentException("throughSequenceInclusive must not be negative");
        }
        Account account = requireAccount(accountId);
        account.lock.lock();
        try {
            return account.entries.stream()
                    .filter(entry -> entry.sequence() <= throughSequenceInclusive)
                    .map(LedgerEntry::signedAmount)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
        } finally {
            account.lock.unlock();
        }
    }

    private LedgerTransaction record(String key, AccountId from, AccountId to, BigDecimal amount,
                                     TransactionType type, UUID reversalOf) {
        LedgerTransaction.requireKey(key);
        LedgerEntry.validateAmount(amount);
        Objects.requireNonNull(type, "type must not be null");
        Account fromAccount = requireAccount(from);
        Account toAccount = requireAccount(to);
        ReentrantLock idempotencyLock = idempotencyLocks[Math.floorMod(key.hashCode(), IDEMPOTENCY_LOCK_STRIPES)];

        idempotencyLock.lock();
        try {
            LedgerTransaction prior = transactionsByKey.get(key);
            if (prior != null) {
                validateSameRequest(prior, from, to, amount, type, reversalOf);
                return prior;
            }
            lockAccounts(from, fromAccount, to, toAccount);
            try {
                if (reversalOf != null && reversalByOriginalId.containsKey(reversalOf)) {
                    throw new IllegalStateException("transaction has already been reversed: " + reversalOf);
                }
                Instant now = Instant.now();
                UUID transactionId = UUID.randomUUID();
                LedgerEntry debit = new LedgerEntry(UUID.randomUUID(), transactionId, sequence.incrementAndGet(),
                        from, EntryDirection.DEBIT, amount, now);
                LedgerEntry credit = new LedgerEntry(UUID.randomUUID(), transactionId, sequence.incrementAndGet(),
                        to, EntryDirection.CREDIT, amount, now);
                LedgerTransaction transaction = new LedgerTransaction(transactionId, key, from, to, amount, type,
                        now, List.of(debit, credit), reversalOf);

                fromAccount.append(debit);
                toAccount.append(credit);
                transactionsByKey.put(key, transaction);
                transactionsById.put(transactionId, transaction);
                if (reversalOf != null) {
                    reversalByOriginalId.put(reversalOf, transactionId);
                }
                return transaction;
            } finally {
                unlockAccounts(from, fromAccount, to, toAccount);
            }
        } finally {
            idempotencyLock.unlock();
        }
    }

    private void validateSameRequest(LedgerTransaction prior, AccountId from, AccountId to, BigDecimal amount,
                                     TransactionType type, UUID reversalOf) {
        if (!prior.fromAccount().equals(from) || !prior.toAccount().equals(to)
                || prior.amount().compareTo(amount) != 0 || prior.type() != type
                || !Objects.equals(prior.reversalOf(), reversalOf)) {
            throw new IllegalArgumentException("idempotency key was already used for a different request");
        }
    }

    private LedgerTransaction requireTransaction(UUID transactionId) {
        LedgerTransaction transaction = transactionsById.get(Objects.requireNonNull(transactionId, "transactionId must not be null"));
        if (transaction == null) {
            throw new IllegalArgumentException("unknown transaction: " + transactionId);
        }
        return transaction;
    }

    private Account requireAccount(AccountId accountId) {
        Account account = accounts.get(Objects.requireNonNull(accountId, "accountId must not be null"));
        if (account == null) {
            throw new IllegalArgumentException("unknown account: " + accountId.value());
        }
        return account;
    }

    private static void lockAccounts(AccountId firstId, Account first, AccountId secondId, Account second) {
        if (first == second) {
            first.lock.lock();
        } else if (firstId.compareTo(secondId) < 0) {
            first.lock.lock(); second.lock.lock();
        } else {
            second.lock.lock(); first.lock.lock();
        }
    }

    private static void unlockAccounts(AccountId firstId, Account first, AccountId secondId, Account second) {
        if (first == second) {
            first.lock.unlock();
        } else if (firstId.compareTo(secondId) < 0) {
            second.lock.unlock(); first.lock.unlock();
        } else {
            first.lock.unlock(); second.lock.unlock();
        }
    }

    private static final class Account {
        private final ReentrantLock lock = new ReentrantLock();
        private final List<LedgerEntry> entries = new ArrayList<>();
        private BigDecimal balance = BigDecimal.ZERO;

        private void append(LedgerEntry entry) {
            entries.add(entry);
            balance = balance.add(entry.signedAmount());
        }
    }
}
