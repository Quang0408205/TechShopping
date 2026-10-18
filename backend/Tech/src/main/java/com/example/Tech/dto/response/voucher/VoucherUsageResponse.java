package com.example.Tech.dto.response.voucher;

import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/** One order that used the voucher (admin list). */
public record VoucherUsageResponse(
        Long orderId,
        String orderCode,
        String orderStatus,
        String customerName,
        String customerEmail,
        BigDecimal discountAmount,
        BigDecimal orderTotal,
        LocalDateTime usedAt,
        @Schema(description = "Set when the order was cancelled and the use given back")
        LocalDateTime releasedAt
) {
}
