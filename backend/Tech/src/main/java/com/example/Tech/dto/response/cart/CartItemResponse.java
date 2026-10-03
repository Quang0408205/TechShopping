package com.example.Tech.dto.response.cart;

import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record CartItemResponse(
        Long cartItemId,
        Long variantId,
        Long productId,
        String productName,
        String productSlug,
        String variantName,
        @Schema(description = "Primary image of the product, else its first image; null when it has none")
        String imageUrl,
        @Schema(description = "Current price of the variant: the discount price when it is lower, else the price")
        BigDecimal unitPrice,
        @Schema(description = "The price before discount; null when there is no discount")
        BigDecimal originalPrice,
        Integer quantity,
        @Schema(description = "unitPrice × quantity")
        BigDecimal lineTotal,
        @Schema(description = "false when the product was deleted, hidden or has no price; "
                + "such lines are not counted in the subtotal")
        boolean available,
        LocalDateTime addedAt
) {
}
