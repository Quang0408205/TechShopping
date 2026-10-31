package com.example.Tech.dto.response.inventory;

import com.example.Tech.entity.inventory.PurchaseReceiptStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/** A receipt with its lines; the list endpoint returns the same record with {@code items} empty. */
public record PurchaseReceiptResponse(
        Long id,
        String code,
        Integer storeId,
        String storeName,
        Integer supplierId,
        String supplierName,
        PurchaseReceiptStatus status,
        String note,
        int lineCount,
        long totalQuantity,
        BigDecimal totalCost,
        String createdByName,
        LocalDateTime createdAt,
        String completedByName,
        LocalDateTime completedAt,
        List<Item> items) {

    public record Item(Long variantId, String variantName, String sku, Long productId, String productName,
                       int quantity, BigDecimal unitCost, BigDecimal lineTotal, BigDecimal averageCost) {
    }
}
