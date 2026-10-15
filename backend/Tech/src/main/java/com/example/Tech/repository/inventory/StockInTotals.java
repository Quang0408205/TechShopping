package com.example.Tech.repository.inventory;

/** Totals of the IN movements matching a filter (StockMovementRepository.stockInTotals). */
public record StockInTotals(Long quantity, Long stockInCount, Long variantCount, Long supplierCount) {
}
