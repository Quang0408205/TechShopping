package com.example.Tech.dto.response.product;

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
        LocalDateTime updatedAt
) {
}
