package com.example.Tech.dto.response.inventory;

import java.time.LocalDateTime;
import java.util.List;

/**
 * One variant's stock across every store (ADMIN "Tất cả chi nhánh" view). stores lists only the stores that ever
 * stocked the variant, by store name; totalQuantity is their sum (closed stores included).
 */
public record InventoryOverviewResponse(
        Long variantId,
        String variantName,
        String skuVariant,
        Long productId,
        String productName,
        String imageUrl,
        int totalQuantity,
        List<StoreStock> stores
) {

    public record StoreStock(
            Integer storeId,
            String storeName,
            boolean storeActive,
            int quantity,
            LocalDateTime updatedAt
    ) {
    }
}
