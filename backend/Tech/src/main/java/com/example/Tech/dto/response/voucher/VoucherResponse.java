package com.example.Tech.dto.response.voucher;

import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public record VoucherResponse(
        Long id,
        String code,
        String name,
        String description,
        BigDecimal discountPercent,
        BigDecimal maxDiscount,
        BigDecimal minOrderAmount,
        LocalDateTime startsAt,
        LocalDateTime endsAt,
        Integer usageLimit,
        Integer perCustomerLimit,
        @Schema(description = "Orders using it that are not cancelled")
        Integer usedCount,
        boolean allCategories,
        List<CategoryRef> categories,
        boolean active,
        @Schema(description = "PAUSED (switched off), UPCOMING, RUNNING, ENDED, or USED_UP (running but no use left)")
        String status,
        @Schema(description = "false once any order used it (even a cancelled one): code locked, delete refused")
        boolean deletable,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {

    public record CategoryRef(Integer id, String name) {
    }
}
