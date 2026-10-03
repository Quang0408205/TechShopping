package com.example.Tech.dto.request.order;

import com.example.Tech.entity.order.OrderStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record OrderStatusUpdateRequest(

        @Schema(description = "New status: PENDING → CONFIRMED → SHIPPING → DELIVERED; CANCELLED from PENDING or "
                + "CONFIRMED. The current status again only updates the tracking number", example = "CONFIRMED")
        @NotNull(message = "Status is required")
        OrderStatus status,

        @Schema(description = "Carrier tracking number; unchanged when omitted, cleared when blank", example = "GHN123456789")
        @Size(max = 100, message = "Tracking number must be at most 100 characters")
        String trackingNumber
) {
}
