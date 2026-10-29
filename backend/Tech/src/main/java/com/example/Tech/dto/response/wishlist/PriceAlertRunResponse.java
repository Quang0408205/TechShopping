package com.example.Tech.dto.response.wishlist;

import io.swagger.v3.oas.annotations.media.Schema;

public record PriceAlertRunResponse(
        @Schema(description = "Saved items looked at") int checked,
        @Schema(description = "Items whose price is now below their mark") int dropped,
        @Schema(description = "Customers who got a price-drop mail") int mailed
) {
}
