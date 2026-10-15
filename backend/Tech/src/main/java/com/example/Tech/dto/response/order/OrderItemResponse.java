package com.example.Tech.dto.response.order;

import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;

public record OrderItemResponse(
        Long orderItemId,
        Long variantId,
        Long productId,
        @Schema(description = "Current catalogue name (orders keep no name snapshot)")
        String productName,
        String productSlug,
        String variantName,
        @Schema(description = "Current best image of the product; null when it has none")
        String imageUrl,
        @Schema(description = "Price charged per unit at checkout")
        BigDecimal unitPrice,
        @Schema(description = "List price per unit at checkout when it was discounted, else null")
        BigDecimal originalPrice,
        int quantity,
        @Schema(description = "(originalPrice − unitPrice) × quantity")
        BigDecimal discountAmount,
        @Schema(description = "unitPrice × quantity")
        BigDecimal subtotal
) {
}
