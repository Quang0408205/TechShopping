package com.example.Tech.dto.response.wishlist;

import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record WishlistItemResponse(
        Long productId,
        String name,
        String categoryName,
        String imageUrl,
        @Schema(description = "Price shown to customers now (promotions included)")
        BigDecimal price,
        @Schema(description = "List price when the product is cheaper than that; else null")
        BigDecimal originalPrice,
        @Schema(description = "Price when the customer saved the product; null on older rows")
        BigDecimal priceAtAdd,
        boolean inStock,
        LocalDateTime addedAt
) {
}
