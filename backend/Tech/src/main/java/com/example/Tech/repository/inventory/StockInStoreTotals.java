package com.example.Tech.repository.inventory;

import java.time.LocalDateTime;

/** IN movements of one store matching a filter (StockMovementRepository.stockInTotalsByStore). */
public record StockInStoreTotals(Integer storeId, Long quantity, Long stockInCount, Long variantCount,
                                 LocalDateTime lastStockInAt) {
}
