package com.attri.systemdesign.lld.ledger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.concurrent.Callable;
import java.util.concurrent.Executors;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class LedgerServiceTest {
    private final AccountId alice = new AccountId("alice");
    private final AccountId merchant = new AccountId("merchant");
    private LedgerService ledger;

    @BeforeEach
    void setUp() {
        ledger = new LedgerService();
        ledger.createAccount(alice);
        ledger.createAccount(merchant);
    }

    @Test
    void createsBalancedImmutableDoubleEntryAndRebuildsBalanceFromHistory() {
        LedgerTransaction transaction = ledger.recordTransaction("payment-1", alice, merchant,
                new BigDecimal("12.50"), TransactionType.PAYMENT);

        assertThat(transaction.isBalanced()).isTrue();
        assertThat(transaction.entries()).hasSize(2);
        assertThat(ledger.getBalance(alice)).isEqualByComparingTo("-12.50");
        assertThat(ledger.getBalance(merchant)).isEqualByComparingTo("12.50");
        assertThat(ledger.reconstructBalance(merchant, Instant.now())).isEqualByComparingTo("12.50");
        assertThat(ledger.reconstructBalance(merchant, transaction.entries().get(1).sequence()))
                .isEqualByComparingTo("12.50");
        assertThat(ledger.getTransactionHistory(alice, 0, 10).entries()).containsExactly(transaction.entries().getFirst());
    }

    @Test
    void returnsOriginalTransactionForAnIdempotentRetryAndRejectsKeyReuse() {
        LedgerTransaction first = ledger.recordTransaction("request-42", alice, merchant,
                BigDecimal.TEN, TransactionType.PAYMENT);
        LedgerTransaction retry = ledger.recordTransaction("request-42", alice, merchant,
                new BigDecimal("10.00"), TransactionType.PAYMENT);

        assertThat(retry).isSameAs(first);
        assertThat(ledger.getBalance(merchant)).isEqualByComparingTo("10");
        assertThatThrownBy(() -> ledger.recordTransaction("request-42", alice, merchant,
                BigDecimal.ONE, TransactionType.PAYMENT))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void reversalPostsEqualAndOppositeEntriesWithoutChangingOriginal() {
        LedgerTransaction original = ledger.recordTransaction("payment-1", alice, merchant,
                BigDecimal.TEN, TransactionType.PAYMENT);
        LedgerTransaction reversal = ledger.reverseTransaction("refund-1", original.id(), TransactionType.REFUND);

        assertThat(reversal.reversalOf()).isEqualTo(original.id());
        assertThat(reversal.isBalanced()).isTrue();
        assertThat(ledger.getBalance(alice)).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(ledger.getBalance(merchant)).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(ledger.getTransactionHistory(alice, 0, 10).totalEntries()).isEqualTo(2);
        assertThatThrownBy(() -> ledger.reverseTransaction("refund-2", original.id(), TransactionType.REFUND))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void concurrentTransactionsProduceTheExactProjectedBalance() throws Exception {
        try (var pool = Executors.newFixedThreadPool(8)) {
            var work = java.util.stream.IntStream.range(0, 200)
                    .<Callable<LedgerTransaction>>mapToObj(i -> () -> ledger.recordTransaction(
                            "concurrent-" + i, alice, merchant, BigDecimal.ONE, TransactionType.PAYMENT))
                    .toList();
            pool.invokeAll(work);
        }

        assertThat(ledger.getBalance(alice)).isEqualByComparingTo("-200");
        assertThat(ledger.getBalance(merchant)).isEqualByComparingTo("200");
        assertThat(ledger.getTransactionHistory(merchant, 1, 50).entries()).hasSize(50);
    }
}
