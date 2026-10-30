package com.example.Tech.entity.payment;

/**
 * Stored as text in installment_orders.status (DB CHECK chk_installment_orders_status).
 * PENDING_APPROVAL (waiting for the partner) → APPROVED (the order counts as paid) → SETTLED (the partner paid the
 * shop); REJECTED by the partner (the order stays and may go to another partner) or CANCELLED (order cancelled).
 * ACTIVE and COMPLETED belong to plans from before partners existed.
 */
public enum InstallmentStatus {

    PENDING_APPROVAL,
    APPROVED,
    REJECTED,
    ACTIVE,
    COMPLETED,
    CANCELLED,
    SETTLED
}
