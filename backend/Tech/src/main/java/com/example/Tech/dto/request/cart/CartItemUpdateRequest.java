package com.example.Tech.dto.request.cart;

import com.example.Tech.service.cart.CartService;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record CartItemUpdateRequest(

        @NotNull(message = "Quantity is required")
        @Min(value = 1, message = "Quantity must be at least 1")
        @Max(value = CartService.MAX_LINE_QUANTITY, message = "Quantity must be at most "
                + CartService.MAX_LINE_QUANTITY)
        @Schema(description = "New quantity of the line (use DELETE to remove it)", example = "2")
        Integer quantity
) {
}
