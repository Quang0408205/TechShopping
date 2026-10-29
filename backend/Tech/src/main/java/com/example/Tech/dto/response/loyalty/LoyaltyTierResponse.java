package com.example.Tech.dto.response.loyalty;

import java.math.BigDecimal;

public record LoyaltyTierResponse(String code, String name, BigDecimal minSpent, BigDecimal pointMultiplier, int sortOrder) {
}
