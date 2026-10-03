package com.example.Tech.dto.response.product;

import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public record ProductVariantResponse(
        Long id,
        Long productId,
        String productName,
        String variantName,
        String skuVariant,
        BigDecimal price,
        BigDecimal discountPrice,
        Integer stockQuantity,
        String color,
        String storage,
        String ram,
        List<AttributeValueResponse> attributeValues,
        LocalDateTime createdAt,
        LocalDateTime updatedAt,

        @Schema(description = "Price charged now (what the cart and checkout use): the lower of discountPrice and "
                + "the active promotion's price, else price")
        BigDecimal effectivePrice,

        @Schema(description = "Name of the promotion giving effectivePrice; null when no promotion applies")
        String activePromotionName
) {
}
