package com.example.Tech.dto.response.product;

import java.time.LocalDateTime;

public record BrandResponse(
        Integer id,
        String name,
        String slug,
        String logoUrl,
        String description,
        String websiteUrl,
        Boolean isActive,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}
