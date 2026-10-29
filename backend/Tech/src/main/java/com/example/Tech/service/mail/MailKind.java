package com.example.Tech.service.mail;

/** The mails about an order; the subject of each is built in {@link OrderMailTemplates}. */
public enum MailKind {
    ORDER_PLACED,
    PAYMENT_RECEIVED,
    ORDER_CONFIRMED,
    ORDER_SHIPPING,
    READY_FOR_PICKUP,
    ORDER_DELIVERED,
    ORDER_CANCELLED
}
