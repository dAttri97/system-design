package com.attri.systemdesign.lld.ledger;

import java.util.List;

/** A stable snapshot page of an account's immutable ledger entries. */
public record AccountHistoryPage(List<LedgerEntry> entries, int pageNumber, int pageSize,
                                 long totalEntries) {
    public AccountHistoryPage {
        entries = List.copyOf(entries);
    }

    public boolean hasNextPage() {
        return (long) (pageNumber + 1) * pageSize < totalEntries;
    }
}
