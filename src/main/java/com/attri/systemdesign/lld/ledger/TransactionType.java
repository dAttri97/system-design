package com.attri.systemdesign.lld.ledger;

/** Business classification. New transaction types can be added without changing ledger mechanics. */
public enum TransactionType {
    PAYMENT, REFUND, FEE, TOP_UP, TRANSFER
}
