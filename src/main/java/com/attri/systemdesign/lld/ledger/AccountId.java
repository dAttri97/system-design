package com.attri.systemdesign.lld.ledger;

import java.util.Objects;

/** Stable identifier for a ledger account. */
public record AccountId(String value) implements Comparable<AccountId> {
    public AccountId {
        Objects.requireNonNull(value, "account id must not be null");
        if (value.isBlank()) {
            throw new IllegalArgumentException("account id must not be blank");
        }
    }

    @Override
    public int compareTo(AccountId other) {
        return value.compareTo(other.value);
    }
}
