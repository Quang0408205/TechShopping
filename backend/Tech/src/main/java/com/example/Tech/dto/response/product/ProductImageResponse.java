package com.example.Tech.dto.response.product;

import java.time.LocalDateTime;

public record ProductImageResponse(
        Long id,
        Long productId,
        String imageUrl,
        String altText,
        Integer displayOrder,
        Boolean isPrimary,
        LocalDateTime uploadedAt
) {
}
