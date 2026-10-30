package com.example.Tech.dto.response.voucher;

import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/** A voucher as the customer sees it at checkout, evaluated against the current cart. */
public record VoucherOptionResponse(
        Long id,
        String code,
        String name,
        String description,
        BigDecimal discountPercent,
        @Schema(description = "Cap on the discount; null = no cap")
        BigDecimal maxDiscount,
        BigDecimal minOrderAmount,
        LocalDateTime endsAt,
        boolean allCategories,
        @Schema(description = "Names of the categories it applies to (empty when allCategories)")
        List<String> categoryNames,
        @Schema(description = "Uses left in all; null = unlimited")
        Integer remainingUses,
        @Schema(description = "true when it can be used on the current cart")
        boolean applicable,
        @Schema(description = "Why not: INACTIVE, NOT_STARTED, EXPIRED, USED_UP, CUSTOMER_LIMIT, NO_ELIGIBLE_ITEMS, BELOW_MINIMUM")
        String problem,
        @Schema(description = "The reason in Vietnamese, ready to show")
        String message,
        @Schema(description = "Cart goods in the voucher's scope (after product promotions)")
        BigDecimal eligibleSubtotal,
        @Schema(description = "Amount taken off the current cart (0 when not applicable)")
        BigDecimal discount,
        @Schema(description = "BELOW_MINIMUM: in-scope goods still to add")
        BigDecimal shortfall
) {
}
