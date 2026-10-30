package com.example.Tech.dto.response.product;

import io.swagger.v3.oas.annotations.media.Schema;

/** How many units of a product were sold (orders not cancelled) and whether it is among the shop's best sellers. */
public record ProductSalesResponse(
        Long productId,
        @Schema(description = "Units in orders that are not cancelled") long sold,
        @Schema(description = "Among the shop's top sellers (at least 2 units sold)") boolean bestSeller
) {
}
