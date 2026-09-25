package com.example.Tech.dto.response.product;

import java.time.LocalDateTime;

public record CategoryResponse(
        Integer id,
        String name,
        String slug,
        String description,
        Integer parentId,
        String parentName,
        String iconUrl,
        Integer displayOrder,
        Boolean isActive,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}
