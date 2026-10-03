package com.example.Tech.dto.response.product;

import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.time.LocalDateTime;

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
        String activePromotionName
) {
}
