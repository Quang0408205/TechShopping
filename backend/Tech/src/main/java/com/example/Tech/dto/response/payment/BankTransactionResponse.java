package com.example.Tech.dto.response.payment;

import com.example.Tech.entity.payment.BankTransactionStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record BankTransactionResponse(
        Long id,
        String externalId,
        String provider,
        BigDecimal amount,
        String content,
        String accountNumber,
        LocalDateTime transferredAt,
        LocalDateTime receivedAt,
        BankTransactionStatus status,
        String reason,
        Long orderId,
        String orderCode,
        LocalDateTime resolvedAt,
        String note
) {
}
