package com.example.Tech.dto.response.cart;

import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public record CartResponse(
        @Schema(description = "Lines in the order they were added")
        List<CartItemResponse> items,
        @Schema(description = "Sum of the quantities of all lines (header counter)")
        int totalQuantity,
        @Schema(description = "Sum of lineTotal over the available lines; shipping is not included")
        BigDecimal subtotal,
        @Schema(description = "Combo discount over the available lines (add-ons bought with their main product)")
        BigDecimal comboDiscount,
        @Schema(description = "Combos applied now")
        List<ComboApplied> combos,
        @Schema(description = "Shipping fee checkout will charge: 30000, free from 10000000, 0 without available lines")
        BigDecimal shippingFee,
        @Schema(description = "subtotal − comboDiscount + shippingFee (shipping is worked out on the subtotal)")
        BigDecimal total,
        @Schema(description = "true when at least one line is not available")
        boolean hasUnavailableItems,
        @Schema(description = "null when the user has no cart yet")
        LocalDateTime updatedAt
) {

    public static CartResponse empty() {
        return new CartResponse(List.of(), 0, BigDecimal.ZERO, BigDecimal.ZERO, List.of(), BigDecimal.ZERO,
                BigDecimal.ZERO, false, null);
    }

    public record ComboApplied(Long comboId, String name, int units, BigDecimal discount) {
    }
}
