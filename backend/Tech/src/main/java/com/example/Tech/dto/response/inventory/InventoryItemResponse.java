package com.example.Tech.dto.response.inventory;

import java.time.LocalDateTime;

public record InventoryItemResponse(
        Integer storeId,
        Long variantId,
        String variantName,
        String skuVariant,
        Long productId,
        String productName,
        String imageUrl,
        int quantity,
        LocalDateTime updatedAt
) {
}
