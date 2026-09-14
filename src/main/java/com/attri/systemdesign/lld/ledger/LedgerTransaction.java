package com.attri.systemdesign.lld.ledger;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

/** Immutable transaction envelope containing exactly one debit and one credit entry. */
public record LedgerTransaction(UUID id, String idempotencyKey, AccountId fromAccount,
                                AccountId toAccount, BigDecimal amount, TransactionType type,
                                Instant recordedAt, List<LedgerEntry> entries,
                                UUID reversalOf) {
    public LedgerTransaction {
        Objects.requireNonNull(id, "id must not be null");
        requireKey(idempotencyKey);
        Objects.requireNonNull(fromAccount, "fromAccount must not be null");
        Objects.requireNonNull(toAccount, "toAccount must not be null");
        LedgerEntry.validateAmount(amount);
        Objects.requireNonNull(type, "type must not be null");
        Objects.requireNonNull(recordedAt, "recordedAt must not be null");
        entries = List.copyOf(entries);
        if (entries.size() != 2 || !isBalanced(entries)) {
            throw new IllegalArgumentException("a transaction must contain a balanced debit and credit entry");
        }
        LedgerEntry debit = entries.stream().filter(entry -> entry.direction() == EntryDirection.DEBIT).findFirst().orElseThrow();
        LedgerEntry credit = entries.stream().filter(entry -> entry.direction() == EntryDirection.CREDIT).findFirst().orElseThrow();
        if (!debit.accountId().equals(fromAccount) || !credit.accountId().equals(toAccount)
                || debit.amount().compareTo(amount) != 0 || credit.amount().compareTo(amount) != 0) {
            throw new IllegalArgumentException("entries do not match the transaction accounts and amount");
        }
    }

    public boolean isBalanced() {
        return isBalanced(entries);
    }

    private static boolean isBalanced(List<LedgerEntry> entries) {
        return entries.stream().map(LedgerEntry::signedAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add).signum() == 0
                && entries.stream().filter(e -> e.direction() == EntryDirection.DEBIT).count() == 1
                && entries.stream().filter(e -> e.direction() == EntryDirection.CREDIT).count() == 1;
    }

    static void requireKey(String idempotencyKey) {
        Objects.requireNonNull(idempotencyKey, "idempotency key must not be null");
        if (idempotencyKey.isBlank()) {
            throw new IllegalArgumentException("idempotency key must not be blank");
        }
    }
}
