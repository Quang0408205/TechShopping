package com.example.Tech.dto.response.order;

import io.swagger.v3.oas.annotations.media.Schema;

/** My orders per purchase-history tab. */
public record OrderCountsResponse(
        long all,
        @Schema(description = "PENDING")
        long pending,
        @Schema(description = "CONFIRMED + SHIPPING")
        long inProgress,
        long delivered,
        long cancelled
) {
}
