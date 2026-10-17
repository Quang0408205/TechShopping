package com.example.Tech.dto.response.product;

import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;

@Schema(description = "One variant as a choice on a product card (GET /products only)")
public record ProductVariantOption(
        Long id,
        @Schema(description = "Storage, else RAM, else the variant name") String label,
        String color,
        @Schema(description = "Price charged now (promotion applied)") BigDecimal effectivePrice,
        @Schema(description = "List price before any discount") BigDecimal price,
        @Schema(description = "Units customers can still buy; 0 = sold out") Integer availableQuantity
) {
}
