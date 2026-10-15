package com.example.Tech.entity.payment;

/** Stored as text in payments.status (DB CHECK chk_payments_status). */
public enum PaymentStatus {

    PENDING,
    PAID,
    /** The order was cancelled after the money arrived; staff still has to send it back. */
    REFUND_PENDING,
    REFUNDED,
    CANCELLED
}
