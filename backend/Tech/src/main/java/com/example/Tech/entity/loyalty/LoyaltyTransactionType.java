package com.example.Tech.entity.loyalty;

/** Stored as text in loyalty_transactions.type (DB CHECK chk_loyalty_transactions_type). */
public enum LoyaltyTransactionType {

    /** Order delivered: points credited. */
    EARN,
    /** Points spent at checkout. */
    REDEEM,
    /** Order cancelled / items returned: spent points given back. */
    REDEEM_BACK,
    /** Items returned: the points earned on them taken back. */
    REVOKE,
    /** Manual correction. */
    ADJUST
}
