package com.attri.systemdesign.lld.ledger;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/** One immutable side of a double-entry transaction. */
public record LedgerEntry(UUID id, UUID transactionId, long sequence, AccountId accountId,
                          EntryDirection direction, BigDecimal amount, Instant recordedAt) {
    public LedgerEntry {
        Objects.requireNonNull(id, "id must not be null");
        Objects.requireNonNull(transactionId, "transactionId must not be null");
        Objects.requireNonNull(accountId, "accountId must not be null");
        Objects.requireNonNull(direction, "direction must not be null");
        validateAmount(amount);
        Objects.requireNonNull(recordedAt, "recordedAt must not be null");
    }

    /** Signed value used for balance reconstruction. */
    public BigDecimal signedAmount() {
        return direction == EntryDirection.DEBIT ? amount.negate() : amount;
    }

    static void validateAmount(BigDecimal amount) {
        Objects.requireNonNull(amount, "amount must not be null");
        if (amount.signum() <= 0) {
            throw new IllegalArgumentException("amount must be positive");
        }
    }
}
