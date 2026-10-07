package com.example.Tech.service.inventory;

import com.example.Tech.dto.request.inventory.InventorySearchRequest;
import com.example.Tech.dto.request.inventory.StockInRequest;
import com.example.Tech.dto.request.inventory.StockInStatsRequest;
import com.example.Tech.dto.response.common.PageResponse;
import com.example.Tech.dto.response.inventory.InventoryItemResponse;
import com.example.Tech.dto.response.inventory.InventoryOverviewResponse;
import com.example.Tech.dto.response.inventory.StockInStatsResponse;
import com.example.Tech.dto.response.inventory.StockMovementResponse;
import org.springframework.data.domain.Pageable;

/**
 * Per-store stock (Phase 7). Every method is store-scoped through StoreAccessGuard: ADMIN → any store,
 * STAFF → only the store of their current assignment. Every quantity change also appends a stock_movements row.
 */
public interface InventoryService {

    PageResponse<InventoryItemResponse> list(Long userId, Integer storeId, InventorySearchRequest filter,
                                             Pageable pageable);

    /** Nhập kho: one step, applied at once; creates the store's row for the variant on its first stock-in. */
    InventoryItemResponse stockIn(Long userId, Integer storeId, StockInRequest request);

    PageResponse<StockMovementResponse> movements(Long userId, Integer storeId, Long variantId, Pageable pageable);

    /** ADMIN only: every variant stocked anywhere, with its quantity per store and in total. */
    PageResponse<InventoryOverviewResponse> overview(Long userId, InventorySearchRequest filter, Pageable pageable);

    /** ADMIN only: goods received (IN movements) over a period, in total and per store. */
    StockInStatsResponse stockInStats(Long userId, StockInStatsRequest filter);
}
