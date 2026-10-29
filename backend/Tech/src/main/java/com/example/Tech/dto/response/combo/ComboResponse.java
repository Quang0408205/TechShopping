package com.example.Tech.dto.response.combo;

import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public record ComboResponse(
        Long id,
        String name,
        String description,
        ProductRef mainProduct,
        List<Item> items,
        LocalDateTime startsAt,
        LocalDateTime endsAt,
        boolean active,
        @Schema(description = "PAUSED (switched off), UPCOMING, RUNNING or ENDED")
        String status,
        @Schema(description = "Orders the combo was applied to")
        long orderCount,
        @Schema(description = "false once any order used it: delete refused, switch it off instead")
        boolean deletable,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {

    public record ProductRef(Long id, String name, String slug, String imageUrl, BigDecimal price) {
    }

    public record Item(ProductRef product, String discountType, BigDecimal discountValue) {
    }
}
