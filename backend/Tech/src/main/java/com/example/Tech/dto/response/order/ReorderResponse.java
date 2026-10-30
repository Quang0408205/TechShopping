package com.example.Tech.dto.response.order;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

/** What went into the cart and what was left out (with the reason). */
public record ReorderResponse(
        List<Line> added,
        List<Line> skipped,
        @Schema(description = "Units in the cart afterwards (for the header badge)")
        int cartQuantity
) {

    public record Line(Long variantId, String productName, String variantName, int quantity, String reason) {
    }
}
