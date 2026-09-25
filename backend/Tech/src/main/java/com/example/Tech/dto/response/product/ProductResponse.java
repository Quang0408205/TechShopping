package com.example.Tech.dto.response.product;

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
        LocalDateTime updatedAt
) {
}
