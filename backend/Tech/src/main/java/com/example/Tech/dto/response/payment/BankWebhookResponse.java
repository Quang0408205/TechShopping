package com.example.Tech.dto.response.payment;

import com.example.Tech.entity.payment.BankTransactionStatus;

/** Answer to the bank: {@code duplicate} = this transaction id was already stored (nothing changed). */
public record BankWebhookResponse(boolean duplicate, BankTransactionStatus status, String orderCode) {
}
