package com.example.Tech.dto.response.inventory;

import com.example.Tech.entity.inventory.StockTransferStatus;

import java.time.LocalDateTime;
import java.util.List;

/** A transfer with its lines; the list endpoint returns the same record with {@code items} empty. */
public record StockTransferResponse(
        Long id,
        String code,
        Integer fromStoreId,
        String fromStoreName,
        Integer toStoreId,
        String toStoreName,
        StockTransferStatus status,
        String note,
        int lineCount,
        long totalQuantity,
        String createdByName,
        LocalDateTime createdAt,
        String sentByName,
        LocalDateTime sentAt,
        String receivedByName,
        LocalDateTime receivedAt,
        String cancelledByName,
        LocalDateTime cancelledAt,
        List<Item> items) {

    public record Item(Long variantId, String variantName, String sku, Long productId, String productName,
                       int quantity) {
    }
}
