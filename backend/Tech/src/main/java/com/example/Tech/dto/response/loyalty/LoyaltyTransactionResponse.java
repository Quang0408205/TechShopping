package com.example.Tech.dto.response.loyalty;

import com.example.Tech.entity.loyalty.LoyaltyTransactionType;

import java.time.LocalDateTime;

public record LoyaltyTransactionResponse(Long id, LoyaltyTransactionType type, int points, int balanceAfter, String note,
                                         Long orderId, String orderCode, LocalDateTime createdAt) {
}
