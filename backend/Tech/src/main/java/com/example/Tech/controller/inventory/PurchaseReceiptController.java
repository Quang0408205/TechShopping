package com.example.Tech.controller.inventory;

import com.example.Tech.dto.request.inventory.PurchaseReceiptRequest;
import com.example.Tech.dto.response.common.ApiResult;
import com.example.Tech.dto.response.common.PageResponse;
import com.example.Tech.dto.response.inventory.PurchaseReceiptResponse;
import com.example.Tech.entity.inventory.PurchaseReceiptStatus;
import com.example.Tech.service.inventory.PurchaseReceiptService;
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
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Goods receipts of one store. STAFF, BRANCH_MANAGER and ADMIN by URL and @PreAuthorize; the service limits staff to
 * the store of their current assignment (StoreAccessGuard).
 */
@RestController
@RequestMapping("/api/v1/admin/stores/{storeId}/purchase-receipts")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('STAFF', 'BRANCH_MANAGER', 'ADMIN')")
@Tag(name = "Admin - Purchase receipts", description = "Multi-line goods receipts with purchase prices")
@ApiResponses({
        @ApiResponse(responseCode = "401", description = "UNAUTHORIZED, INVALID_TOKEN"),
        @ApiResponse(responseCode = "403", description = "ACCESS_DENIED (another store), NO_ACTIVE_STORE_ASSIGNMENT"),
        @ApiResponse(responseCode = "404", description = "STORE_NOT_FOUND, PURCHASE_RECEIPT_NOT_FOUND")
})
public class PurchaseReceiptController {

    private final PurchaseReceiptService receiptService;

    @GetMapping
    @Operation(summary = "Receipts of the store, newest first; keyword matches the supplier name")
    public ResponseEntity<ApiResult<PageResponse<PurchaseReceiptResponse>>> list(
            @Parameter(hidden = true) @AuthenticationPrincipal Jwt jwt,
            @PathVariable Integer storeId,
            @RequestParam(required = false) PurchaseReceiptStatus status,
            @RequestParam(required = false) String keyword,
            @ParameterObject @PageableDefault(size = 20, sort = {"createdAt", "id"},
                    direction = Sort.Direction.DESC) Pageable pageable) {
        return ResponseEntity.ok(ApiResult.ok(receiptService.list(userId(jwt), storeId, status, keyword, pageable)));
    }

    @GetMapping("/{receiptId}")
    @Operation(summary = "One receipt with its lines")
    public ResponseEntity<ApiResult<PurchaseReceiptResponse>> get(
            @Parameter(hidden = true) @AuthenticationPrincipal Jwt jwt,
            @PathVariable Integer storeId, @PathVariable Long receiptId) {
        return ResponseEntity.ok(ApiResult.ok(receiptService.get(userId(jwt), storeId, receiptId)));
    }

    @PostMapping
    @Operation(summary = "Create a draft receipt; the stock does not change until it is completed")
    @ApiResponses({
            @ApiResponse(responseCode = "400", description = "VALIDATION_ERROR (empty, a variant twice)"),
            @ApiResponse(responseCode = "404", description = "SUPPLIER_NOT_FOUND, PRODUCT_VARIANT_NOT_FOUND"),
            @ApiResponse(responseCode = "409", description = "SUPPLIER_INACTIVE")
    })
    public ResponseEntity<ApiResult<PurchaseReceiptResponse>> create(
            @Parameter(hidden = true) @AuthenticationPrincipal Jwt jwt,
            @PathVariable Integer storeId, @Valid @RequestBody PurchaseReceiptRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResult.ok(receiptService.create(userId(jwt), storeId, request)));
    }

    @PutMapping("/{receiptId}")
    @Operation(summary = "Replace the content of a draft receipt")
    @ApiResponse(responseCode = "409", description = "PURCHASE_RECEIPT_NOT_DRAFT, SUPPLIER_INACTIVE")
    public ResponseEntity<ApiResult<PurchaseReceiptResponse>> update(
            @Parameter(hidden = true) @AuthenticationPrincipal Jwt jwt,
            @PathVariable Integer storeId, @PathVariable Long receiptId,
            @Valid @RequestBody PurchaseReceiptRequest request) {
        return ResponseEntity.ok(ApiResult.ok(receiptService.update(userId(jwt), storeId, receiptId, request)));
    }

    @PostMapping("/{receiptId}/complete")
    @Operation(summary = "Complete a draft: adds the stock, writes the stock ledger, updates the average costs")
    @ApiResponse(responseCode = "409", description = "PURCHASE_RECEIPT_NOT_DRAFT")
    public ResponseEntity<ApiResult<PurchaseReceiptResponse>> complete(
            @Parameter(hidden = true) @AuthenticationPrincipal Jwt jwt,
            @PathVariable Integer storeId, @PathVariable Long receiptId) {
        return ResponseEntity.ok(ApiResult.ok(receiptService.complete(userId(jwt), storeId, receiptId)));
    }

    @PostMapping("/{receiptId}/cancel")
    @Operation(summary = "Cancel a draft receipt")
    @ApiResponse(responseCode = "409", description = "PURCHASE_RECEIPT_NOT_DRAFT")
    public ResponseEntity<ApiResult<PurchaseReceiptResponse>> cancel(
            @Parameter(hidden = true) @AuthenticationPrincipal Jwt jwt,
            @PathVariable Integer storeId, @PathVariable Long receiptId) {
        return ResponseEntity.ok(ApiResult.ok(receiptService.cancel(userId(jwt), storeId, receiptId)));
    }

    private static Long userId(Jwt jwt) {
        return Long.valueOf(jwt.getSubject());
    }
}
