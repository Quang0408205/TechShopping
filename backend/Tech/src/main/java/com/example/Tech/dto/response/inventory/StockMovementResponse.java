package com.example.Tech.dto.response.inventory;

import com.example.Tech.entity.inventory.MovementType;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;

public record StockMovementResponse(
        Long id,
        MovementType type,

        @Schema(description = "Positive for IN / RETURN, negative for OUT")
        int quantityChange,

        String supplierName,
        String note,
        Long orderId,

        @Schema(example = "DH00000042")
        String orderCode,

        @Schema(description = "Null for changes made by the system")
        Long createdById,

        String createdByName,
        LocalDateTime createdAt
) {
}
