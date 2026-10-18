package com.example.Tech.dto.response.order;

import io.swagger.v3.oas.annotations.media.Schema;

/** Plan v5 GĐ6: my orders per purchase-history tab. */
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
