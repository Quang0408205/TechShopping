package com.example.Tech.dto.response.product;

/** Plan v8: units of one variant held by one open store (only stores with stock are listed). */
public record ProductStoreStockResponse(
        Long variantId,
        Integer storeId,
        String storeName,
        String address,
        String district,
        String city,
        int quantity
) {
}
