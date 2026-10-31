package com.example.Tech.controller.inventory;

import com.example.Tech.dto.request.inventory.StockTransferRequest;
import com.example.Tech.dto.response.common.ApiResult;
import com.example.Tech.dto.response.common.PageResponse;
import com.example.Tech.dto.response.inventory.StockTransferResponse;
import com.example.Tech.entity.inventory.StockTransferStatus;
import com.example.Tech.service.inventory.StockTransferService;
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
 * Transfers of goods between stores. STAFF, BRANCH_MANAGER and ADMIN by URL and @PreAuthorize; the service limits staff
 * to the transfers of their own store and to the step that belongs to their side (StoreAccessGuard).
 */
@RestController
@RequestMapping("/api/v1/admin/stock-transfers")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('STAFF', 'BRANCH_MANAGER', 'ADMIN')")
@Tag(name = "Admin - Stock transfers", description = "Goods moving between stores")
@ApiResponses({
        @ApiResponse(responseCode = "401", description = "UNAUTHORIZED, INVALID_TOKEN"),
        @ApiResponse(responseCode = "403", description = "ACCESS_DENIED (the other side's step or another store), NO_ACTIVE_STORE_ASSIGNMENT"),
        @ApiResponse(responseCode = "404", description = "STOCK_TRANSFER_NOT_FOUND, STORE_NOT_FOUND")
})
public class StockTransferController {

    private final StockTransferService transferService;

    @GetMapping
    @Operation(summary = "Transfers of the caller's store (ADMIN: all, optionally one store), newest first")
    public ResponseEntity<ApiResult<PageResponse<StockTransferResponse>>> list(
            @Parameter(hidden = true) @AuthenticationPrincipal Jwt jwt,
            @RequestParam(required = false) StockTransferService.Direction direction,
            @RequestParam(required = false) StockTransferStatus status,
            @RequestParam(required = false) Integer storeId,
            @ParameterObject @PageableDefault(size = 20, sort = {"createdAt", "id"},
                    direction = Sort.Direction.DESC) Pageable pageable) {
        return ResponseEntity.ok(ApiResult.ok(transferService.list(userId(jwt), direction, status, storeId, pageable)));
    }

    @GetMapping("/{id}")
    @Operation(summary = "One transfer with its lines")
    public ResponseEntity<ApiResult<StockTransferResponse>> get(
            @Parameter(hidden = true) @AuthenticationPrincipal Jwt jwt, @PathVariable Long id) {
        return ResponseEntity.ok(ApiResult.ok(transferService.get(userId(jwt), id)));
    }

    @PostMapping
    @Operation(summary = "Create a draft transfer from the caller's store; the stock does not change until it is sent")
    @ApiResponse(responseCode = "400", description = "VALIDATION_ERROR (same store, closed store, a variant twice)")
    public ResponseEntity<ApiResult<StockTransferResponse>> create(
            @Parameter(hidden = true) @AuthenticationPrincipal Jwt jwt, @Valid @RequestBody StockTransferRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResult.ok(transferService.create(userId(jwt), request)));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Replace the content of a draft transfer (sending store)")
    @ApiResponse(responseCode = "409", description = "STOCK_TRANSFER_INVALID_STATUS")
    public ResponseEntity<ApiResult<StockTransferResponse>> update(
            @Parameter(hidden = true) @AuthenticationPrincipal Jwt jwt, @PathVariable Long id,
            @Valid @RequestBody StockTransferRequest request) {
        return ResponseEntity.ok(ApiResult.ok(transferService.update(userId(jwt), id, request)));
    }

    @PostMapping("/{id}/send")
    @Operation(summary = "Send a draft: takes the goods out of the sending store (sending store)")
    @ApiResponse(responseCode = "409", description = "STOCK_TRANSFER_INVALID_STATUS, STOCK_TRANSFER_INSUFFICIENT_STOCK")
    public ResponseEntity<ApiResult<StockTransferResponse>> send(
            @Parameter(hidden = true) @AuthenticationPrincipal Jwt jwt, @PathVariable Long id) {
        return ResponseEntity.ok(ApiResult.ok(transferService.send(userId(jwt), id)));
    }

    @PostMapping("/{id}/receive")
    @Operation(summary = "Receive a transfer on the road: adds the goods to the receiving store (receiving store)")
    @ApiResponse(responseCode = "409", description = "STOCK_TRANSFER_INVALID_STATUS")
    public ResponseEntity<ApiResult<StockTransferResponse>> receive(
            @Parameter(hidden = true) @AuthenticationPrincipal Jwt jwt, @PathVariable Long id) {
        return ResponseEntity.ok(ApiResult.ok(transferService.receive(userId(jwt), id)));
    }

    @PostMapping("/{id}/cancel")
    @Operation(summary = "Cancel a draft or a transfer on the road; goods already sent go back (sending store)")
    @ApiResponse(responseCode = "409", description = "STOCK_TRANSFER_INVALID_STATUS")
    public ResponseEntity<ApiResult<StockTransferResponse>> cancel(
            @Parameter(hidden = true) @AuthenticationPrincipal Jwt jwt, @PathVariable Long id) {
        return ResponseEntity.ok(ApiResult.ok(transferService.cancel(userId(jwt), id)));
    }

    private static Long userId(Jwt jwt) {
        return Long.valueOf(jwt.getSubject());
    }
}
