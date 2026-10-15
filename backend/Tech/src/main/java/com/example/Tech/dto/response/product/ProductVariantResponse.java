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
        String activePromotionName,
        @Schema(description = "Units customers can still buy: this variant's stock in OPEN stores (plan v3); 0 = out of stock")
        Integer availableQuantity
) {

    /** The same variant with a fresh stock figure (the rest of a cached response may be minutes old). */
    public ProductVariantResponse withAvailableQuantity(Integer available) {
        return new ProductVariantResponse(id, productId, productName, variantName, skuVariant, price, discountPrice,
                stockQuantity, color, storage, ram, attributeValues, createdAt, updatedAt, effectivePrice,
                activePromotionName, available);
    }
}
