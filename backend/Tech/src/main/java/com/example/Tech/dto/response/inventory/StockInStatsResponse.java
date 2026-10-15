package com.example.Tech.dto.response.inventory;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Goods received (IN movements) over a period: totals, then one row per store, most received first. Stock-in only
 * records quantities (no purchase price), so there is no money amount. supplierCount counts distinct supplier names
 * (case-insensitive); stock-ins without a supplier are not counted there.
 */
public record StockInStatsResponse(
        Integer storeId,
        LocalDate fromDate,
        LocalDate toDate,
        long totalQuantity,
        long stockInCount,
        long variantCount,
        long supplierCount,
        List<StoreStockIn> stores
) {

    /** Open stores are listed even without stock-ins in the period; closed ones only when they had some. */
    public record StoreStockIn(
            Integer storeId,
            String storeName,
            boolean storeActive,
            long quantity,
            long stockInCount,
            long variantCount,
            LocalDateTime lastStockInAt
    ) {
    }
}
