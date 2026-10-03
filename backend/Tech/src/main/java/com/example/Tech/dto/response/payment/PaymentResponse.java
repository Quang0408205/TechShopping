package com.example.Tech.dto.response.payment;

import com.example.Tech.entity.payment.PaymentStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/** The main payment of a COD / BANK_TRANSFER order. */
public record PaymentResponse(
        Long id,
        PaymentStatus status,
        BigDecimal amount,
        String transactionId,
        LocalDateTime paidAt,
        LocalDateTime refundedAt,
        BankTransferResponse bankTransfer
) {
}
