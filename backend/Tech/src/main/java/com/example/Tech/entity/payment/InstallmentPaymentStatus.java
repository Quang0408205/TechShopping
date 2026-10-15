package com.example.Tech.entity.payment;

/** Stored as text in installment_payments.status; "overdue" is not stored, it is PENDING with a past due date. */
public enum InstallmentPaymentStatus {

    PENDING,
    PAID
}
