package com.example.Tech.controller.inventory;

import com.example.Tech.dto.request.inventory.InventorySearchRequest;
import com.example.Tech.dto.request.inventory.StockInRequest;
import com.example.Tech.dto.response.common.ApiResult;
import com.example.Tech.dto.response.common.PageResponse;
import com.example.Tech.dto.response.inventory.InventoryItemResponse;
import com.example.Tech.dto.response.inventory.StockMovementResponse;
import com.example.Tech.service.inventory.InventoryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
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
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Stock of one store. STAFF + ADMIN by URL (SecurityConfig) and @PreAuthorize; the service then limits a STAFF
 * member to the store of their current assignment (StoreAccessGuard).
 */
@RestController
@RequestMapping("/api/v1/admin/stores/{storeId}/inventory")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('STAFF', 'BRANCH_MANAGER', 'ADMIN')")
@Tag(name = "Admin - Inventory", description = "Per-store stock and stock-in (STAFF of the store, or ADMIN)")
@ApiResponses({
        @ApiResponse(responseCode = "401", description = "UNAUTHORIZED, INVALID_TOKEN"),
        @ApiResponse(responseCode = "403", description = "ACCESS_DENIED (another store), NO_ACTIVE_STORE_ASSIGNMENT, "
                + "ACCOUNT_DISABLED"),
        @ApiResponse(responseCode = "404", description = "STORE_NOT_FOUND")
})
public class StoreInventoryController {

    private final InventoryService inventoryService;

    @GetMapping
    @Operation(summary = "Stock of the store, by product name by default",
            description = "Only variants that were ever stocked in at this store are listed")
    @ApiResponse(responseCode = "200", description = "Page of stock rows")
    public ResponseEntity<ApiResult<PageResponse<InventoryItemResponse>>> list(
            @Parameter(hidden = true) @AuthenticationPrincipal Jwt jwt,
            @PathVariable Integer storeId,
            @Valid @ParameterObject InventorySearchRequest filter,
            @ParameterObject @PageableDefault(size = 20, sort = {"variant.product.name", "variant.variantName"},
                    direction = Sort.Direction.ASC) Pageable pageable) {
        return ResponseEntity.ok(ApiResult.ok(inventoryService.list(userId(jwt), storeId, filter, pageable)));
    }

    @PostMapping("/stock-in")
    @Operation(summary = "Nhập kho: add a quantity of one variant, applied at once")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "The variant's stock at the store after the stock-in"),
            @ApiResponse(responseCode = "400", description = "VALIDATION_ERROR"),
            @ApiResponse(responseCode = "404", description = "PRODUCT_VARIANT_NOT_FOUND")
    })
    public ResponseEntity<ApiResult<InventoryItemResponse>> stockIn(
            @Parameter(hidden = true) @AuthenticationPrincipal Jwt jwt,
            @PathVariable Integer storeId,
            @Valid @RequestBody StockInRequest request) {
        return ResponseEntity.ok(ApiResult.ok(inventoryService.stockIn(userId(jwt), storeId, request)));
    }

    @GetMapping("/{variantId}/movements")
    @Operation(summary = "Stock movement history of one variant at the store, newest first")
    @ApiResponse(responseCode = "200", description = "Page of movements (empty when never stocked here)")
    public ResponseEntity<ApiResult<PageResponse<StockMovementResponse>>> movements(
            @Parameter(hidden = true) @AuthenticationPrincipal Jwt jwt,
            @PathVariable Integer storeId,
            @PathVariable Long variantId,
            @ParameterObject @PageableDefault(size = 10) Pageable pageable) {
        return ResponseEntity.ok(ApiResult.ok(inventoryService.movements(userId(jwt), storeId, variantId, pageable)));
    }

    /** The access token subject is the user id (see JwtTokenService). */
    private static Long userId(Jwt jwt) {
        return Long.valueOf(jwt.getSubject());
    }
}
