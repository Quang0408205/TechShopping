package com.example.Tech.dto.response.cart;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

/** Whether one open store can hand over the whole cart (pickup choice at checkout). */
public record StoreAvailabilityResponse(
        Integer storeId,
        String name,
        String address,
        String district,
        String city,
        @Schema(description = "true when the store holds every unit of the cart")
        boolean available,
        @Schema(description = "\"Product - Variant (cần 2, còn 1)\" for each short line; empty when available")
        List<String> missing
) {
}
