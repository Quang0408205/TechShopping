package com.example.Tech.dto.response.order;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record ShippingFeeRuleResponse(Integer id, String provinceName, BigDecimal fee, LocalDateTime updatedAt) {
}
