package com.example.Tech.controller.inventory;

import com.example.Tech.dto.request.inventory.InventorySearchRequest;
import com.example.Tech.dto.request.inventory.StockInStatsRequest;
import com.example.Tech.dto.response.common.ApiResult;
import com.example.Tech.dto.response.common.PageResponse;
import com.example.Tech.dto.response.inventory.InventoryOverviewResponse;
import com.example.Tech.dto.response.inventory.StockInStatsResponse;
import com.example.Tech.service.inventory.InventoryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Stock of every store at once ("Tất cả chi nhánh") and the goods-received statistics. ADMIN only: by URL
 * (SecurityConfig, /api/v1/admin/**), @PreAuthorize, and the role re-checked in the DB by the service.
 */
@RestController
@RequestMapping("/api/v1/admin/inventory")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
@Tag(name = "Admin - Inventory", description = "Per-store stock and stock-in (STAFF of the store, or ADMIN)")
public class AdminInventoryController {

    private final InventoryService inventoryService;

    @GetMapping
    @Operation(summary = "Stock of every store, one row per variant, by product name by default",
            description = "Only variants stocked in at some store are listed; outOfStock=true keeps the variants "
                    + "no store has left")
    @ApiResponse(responseCode = "200", description = "Page of variants with their quantity per store and in total")
    @ApiResponse(responseCode = "401", description = "UNAUTHORIZED, INVALID_TOKEN")
    @ApiResponse(responseCode = "403", description = "ACCESS_DENIED, ACCOUNT_DISABLED")
    public ResponseEntity<ApiResult<PageResponse<InventoryOverviewResponse>>> overview(
            @Parameter(hidden = true) @AuthenticationPrincipal Jwt jwt,
            @Valid @ParameterObject InventorySearchRequest filter,
            @ParameterObject @PageableDefault(size = 20, sort = {"product.name", "variantName"},
                    direction = Sort.Direction.ASC) Pageable pageable) {
        return ResponseEntity.ok(ApiResult.ok(
                inventoryService.overview(Long.valueOf(jwt.getSubject()), filter, pageable)));
    }

    @GetMapping("/stock-in-stats")
    @Operation(summary = "Goods received (stock-ins) over a period, in total and per store",
            description = "Quantities only: stock-in records no purchase price. Open stores are listed even without "
                    + "stock-ins in the period")
    @ApiResponse(responseCode = "200", description = "Totals and one row per store, most received first")
    @ApiResponse(responseCode = "400", description = "VALIDATION_ERROR (fromDate after toDate)")
    @ApiResponse(responseCode = "401", description = "UNAUTHORIZED, INVALID_TOKEN")
    @ApiResponse(responseCode = "403", description = "ACCESS_DENIED, ACCOUNT_DISABLED")
    @ApiResponse(responseCode = "404", description = "STORE_NOT_FOUND")
    public ResponseEntity<ApiResult<StockInStatsResponse>> stockInStats(
            @Parameter(hidden = true) @AuthenticationPrincipal Jwt jwt,
            @ParameterObject StockInStatsRequest filter) {
        return ResponseEntity.ok(ApiResult.ok(inventoryService.stockInStats(Long.valueOf(jwt.getSubject()), filter)));
    }
}
