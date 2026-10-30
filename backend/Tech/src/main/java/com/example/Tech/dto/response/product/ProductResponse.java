package com.example.Tech.dto.response.product;

import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public record ProductResponse(
        Long id,
        String name,
        String slug,
        String description,
        Integer categoryId,
        String categoryName,
        Integer brandId,
        String brandName,
        BigDecimal basePrice,
        BigDecimal discountPrice,
        Integer stockQuantity,
        String sku,
        BigDecimal weight,
        Integer warrantyMonths,
        BigDecimal rating,
        Integer totalReviews,
        Integer viewCount,
        Boolean isActive,
        LocalDateTime createdAt,
        LocalDateTime updatedAt,

        @Schema(description = "Primary image, else the first image by display order; null when the product has none")
        String primaryImageUrl,

        @Schema(description = "Price shown to customers now: the lower of discountPrice and the active promotion's "
                + "price, else basePrice. discountPrice stays the raw value edited in the admin form")
        BigDecimal effectivePrice,

        @Schema(description = "Name of the promotion giving effectivePrice; null when no promotion applies")
        String activePromotionName,

        @Schema(description = "Units customers can still buy: stock of every variant in OPEN stores (inventory per "
                + "store). 0 = out of stock. stockQuantity is a legacy field and not used for sales")
        Integer availableQuantity,

        @Schema(description = "Variants as card choices, cheapest first; only filled by GET /products (null elsewhere)")
        List<ProductVariantOption> variants
) {

    /** The same product carrying the variant choices of a list card. */
    public ProductResponse withVariants(List<ProductVariantOption> options) {
        return new ProductResponse(id, name, slug, description, categoryId, categoryName, brandId, brandName,
                basePrice, discountPrice, stockQuantity, sku, weight, warrantyMonths, rating, totalReviews, viewCount,
                isActive, createdAt, updatedAt, primaryImageUrl, effectivePrice, activePromotionName,
                availableQuantity, options);
    }

    /** The same product with a fresh stock figure (the rest of a cached response may be minutes old). */
    public ProductResponse withAvailableQuantity(Integer available) {
        return new ProductResponse(id, name, slug, description, categoryId, categoryName, brandId, brandName,
                basePrice, discountPrice, stockQuantity, sku, weight, warrantyMonths, rating, totalReviews, viewCount,
                isActive, createdAt, updatedAt, primaryImageUrl, effectivePrice, activePromotionName, available, variants);
    }
}
