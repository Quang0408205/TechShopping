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
        @Schema(description = "true when at least one line is not available")
        boolean hasUnavailableItems,
        @Schema(description = "null when the user has no cart yet")
        LocalDateTime updatedAt
) {

    public static CartResponse empty() {
        return new CartResponse(List.of(), 0, BigDecimal.ZERO, false, null);
    }
}
