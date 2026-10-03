package com.example.Tech.entity.payment;

/**
 * Stored as text in installment_orders.status (DB CHECK chk_installment_orders_status).
 * PENDING_APPROVAL → APPROVED (staff) → ACTIVE (order delivered, schedule created) → COMPLETED (last period paid);
 * REJECTED (staff, the order is cancelled too) or CANCELLED (order cancelled before delivery).
 */
public enum InstallmentStatus {

    PENDING_APPROVAL,
    APPROVED,
    REJECTED,
    ACTIVE,
    COMPLETED,
    CANCELLED
}
