package com.example.Tech.dto.request.inventory;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Size;

/**
 * Optional filters for a store's inventory list.
 */
public record InventorySearchRequest(

        @Schema(description = "Part of the product name, variant name or SKU", example = "iphone")
        @Size(max = 255, message = "Từ khóa tối đa 255 ký tự")
        String keyword,

        @Schema(description = "true = only variants whose stock is 0", example = "false")
        Boolean outOfStock
) {
}
