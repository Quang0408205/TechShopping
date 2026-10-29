package com.example.Tech.dto.response.product;

import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;

/** One row of the search box suggestions (plan v5 GĐ3). */
public record ProductSuggestionResponse(
        Long id,
        String name,
        String brandName,
        String categoryName,
        @Schema(description = "Primary image, else the first one; null when the product has none")
        String imageUrl,
        @Schema(description = "Price shown to customers now (promotions applied)")
        BigDecimal price,
        @Schema(description = "List price when it is higher than price (shown struck through); null otherwise")
        BigDecimal originalPrice,
        @Schema(description = "false when no open store holds any unit")
        boolean inStock
) {
}
