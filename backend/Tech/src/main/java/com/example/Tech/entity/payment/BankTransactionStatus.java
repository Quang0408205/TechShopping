package com.example.Tech.entity.payment;

/** Stored as text in bank_transactions.status (DB CHECK chk_bank_transactions_status). */
public enum BankTransactionStatus {

    /** The webhook found the order, the amount was right: the transfer was confirmed automatically. */
    MATCHED,
    /** No order code in the transfer content (or no such order). */
    UNMATCHED,
    /** An order was found but the money cannot be applied (wrong amount, cancelled, already paid...). */
    REVIEW,
    /** ADMIN applied the transfer to an order by hand. */
    RESOLVED,
    /** ADMIN decided the transfer has nothing to do with the shop. */
    IGNORED;

    /** Transfers an ADMIN still has to look at. */
    public boolean needsAttention() {
        return this == UNMATCHED || this == REVIEW;
    }
}
