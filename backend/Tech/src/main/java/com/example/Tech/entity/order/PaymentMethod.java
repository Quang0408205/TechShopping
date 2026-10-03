package com.example.Tech.entity.order;

/**
 * How the customer chose to pay, stored as text in orders.payment_method. Phase 4 only records the choice;
 * payments and installment plans are Phase 5.
 */
public enum PaymentMethod {

    COD,
    BANK_TRANSFER,
    INSTALLMENT
}
