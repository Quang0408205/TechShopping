package com.example.Tech.controller.order;

import com.example.Tech.dto.request.order.AdminOrderSearchRequest;
import com.example.Tech.dto.request.order.OrderStatusUpdateRequest;
import com.example.Tech.dto.request.order.OrderStoreRequest;
import com.example.Tech.dto.request.payment.InstallmentRejectRequest;
import com.example.Tech.dto.request.payment.PaymentConfirmRequest;
import com.example.Tech.dto.response.common.ApiResult;
import com.example.Tech.dto.response.common.PageResponse;
import com.example.Tech.dto.response.order.AdminOrderResponse;
import com.example.Tech.service.order.AdminOrderService;
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
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Order management for staff. Protected by URL (/api/v1/admin/orders/** = STAFF or ADMIN in SecurityConfig)
 * and by @PreAuthorize; the service also re-checks the caller's roles in the database and limits a STAFF member
 * to the orders of their store.
 */
@RestController
@RequestMapping("/api/v1/admin/orders")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('STAFF', 'ADMIN')")
@Tag(name = "Admin - Orders", description = "Order management (STAFF and ADMIN)")
@ApiResponses({
        @ApiResponse(responseCode = "401", description = "UNAUTHORIZED, INVALID_TOKEN"),
        @ApiResponse(responseCode = "403", description = "ACCESS_DENIED, ACCOUNT_DISABLED")
})
public class AdminOrderController {

    private final AdminOrderService adminOrderService;

    @GetMapping
    @Operation(summary = "Search orders, newest first by default",
            description = "Filters are optional and combined with AND. Sort example: sort=totalAmount,desc")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Page of orders with their items and customer"),
            @ApiResponse(responseCode = "400", description = "VALIDATION_ERROR / MALFORMED_REQUEST")
    })
    public ResponseEntity<ApiResult<PageResponse<AdminOrderResponse>>> search(
            @Parameter(hidden = true) @AuthenticationPrincipal Jwt jwt,
            @Valid @ParameterObject AdminOrderSearchRequest filter,
            @ParameterObject @PageableDefault(size = 20, sort = "id", direction = Sort.Direction.DESC) Pageable pageable) {
        return ResponseEntity.ok(ApiResult.ok(adminOrderService.search(staffId(jwt), filter, pageable)));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get an order")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Order found"),
            @ApiResponse(responseCode = "404", description = "ORDER_NOT_FOUND")
    })
    public ResponseEntity<ApiResult<AdminOrderResponse>> getById(@Parameter(hidden = true) @AuthenticationPrincipal Jwt jwt,
                                                                 @PathVariable Long id) {
        return ResponseEntity.ok(ApiResult.ok(adminOrderService.getById(staffId(jwt), id)));
    }

    @PatchMapping("/{id}/status")
    @Operation(summary = "Change the status (PENDING → CONFIRMED → SHIPPING → DELIVERED, CANCELLED from "
            + "PENDING / CONFIRMED) and / or the tracking number")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Order updated"),
            @ApiResponse(responseCode = "400", description = "VALIDATION_ERROR, MALFORMED_REQUEST (unknown status)"),
            @ApiResponse(responseCode = "404", description = "ORDER_NOT_FOUND"),
            @ApiResponse(responseCode = "409", description = "INVALID_ORDER_STATUS, PAYMENT_REQUIRED (unpaid bank "
                    + "transfer → CONFIRMED), INSTALLMENT_NOT_APPROVED, ORDER_STORE_MISSING, INSUFFICIENT_STOCK")
    })
    public ResponseEntity<ApiResult<AdminOrderResponse>> updateStatus(
            @Parameter(hidden = true) @AuthenticationPrincipal Jwt jwt,
            @PathVariable Long id,
            @Valid @RequestBody OrderStatusUpdateRequest request) {
        return ResponseEntity.ok(ApiResult.ok(adminOrderService.updateStatus(staffId(jwt), id, request)));
    }

    @PostMapping("/{id}/payment/confirm")
    @Operation(summary = "Bank transfer received (PENDING → PAID); COD is marked paid automatically on delivery")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Payment confirmed"),
            @ApiResponse(responseCode = "400", description = "VALIDATION_ERROR (transactionId > 100)"),
            @ApiResponse(responseCode = "404", description = "ORDER_NOT_FOUND, PAYMENT_NOT_FOUND (installment order)"),
            @ApiResponse(responseCode = "409", description = "INVALID_PAYMENT_STATUS (not pending, or COD)")
    })
    public ResponseEntity<ApiResult<AdminOrderResponse>> confirmPayment(
            @Parameter(hidden = true) @AuthenticationPrincipal Jwt jwt,
            @PathVariable Long id,
            @Valid @RequestBody(required = false) PaymentConfirmRequest request) {
        return ResponseEntity.ok(ApiResult.ok(adminOrderService.confirmPayment(staffId(jwt), id, request)));
    }

    @PostMapping("/{id}/payment/refund")
    @Operation(summary = "Money of a cancelled paid order sent back (REFUND_PENDING → REFUNDED)")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Refund recorded"),
            @ApiResponse(responseCode = "404", description = "ORDER_NOT_FOUND, PAYMENT_NOT_FOUND"),
            @ApiResponse(responseCode = "409", description = "INVALID_PAYMENT_STATUS")
    })
    public ResponseEntity<ApiResult<AdminOrderResponse>> refundPayment(
            @Parameter(hidden = true) @AuthenticationPrincipal Jwt jwt,
            @PathVariable Long id) {
        return ResponseEntity.ok(ApiResult.ok(adminOrderService.refundPayment(staffId(jwt), id)));
    }

    @PostMapping("/{id}/installment/approve")
    @Operation(summary = "Approve the installment plan (needed before the order can be CONFIRMED)")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Plan approved"),
            @ApiResponse(responseCode = "404", description = "ORDER_NOT_FOUND, INSTALLMENT_NOT_FOUND"),
            @ApiResponse(responseCode = "409", description = "INVALID_INSTALLMENT_STATUS")
    })
    public ResponseEntity<ApiResult<AdminOrderResponse>> approveInstallment(
            @Parameter(hidden = true) @AuthenticationPrincipal Jwt jwt,
            @PathVariable Long id) {
        return ResponseEntity.ok(ApiResult.ok(adminOrderService.approveInstallment(staffId(jwt), id)));
    }

    @PostMapping("/{id}/installment/reject")
    @Operation(summary = "Reject the installment plan with a reason; the order is cancelled")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Plan rejected, order cancelled"),
            @ApiResponse(responseCode = "400", description = "VALIDATION_ERROR (reason required, ≤ 1000)"),
            @ApiResponse(responseCode = "404", description = "ORDER_NOT_FOUND, INSTALLMENT_NOT_FOUND"),
            @ApiResponse(responseCode = "409", description = "INVALID_INSTALLMENT_STATUS")
    })
    public ResponseEntity<ApiResult<AdminOrderResponse>> rejectInstallment(
            @Parameter(hidden = true) @AuthenticationPrincipal Jwt jwt,
            @PathVariable Long id,
            @Valid @RequestBody InstallmentRejectRequest request) {
        return ResponseEntity.ok(ApiResult.ok(adminOrderService.rejectInstallment(staffId(jwt), id, request)));
    }

    @PatchMapping("/{id}/store")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "ADMIN: move a pending order to another open store (e.g. after INSUFFICIENT_STOCK)")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Order moved; a pickup order gets the new store's address"),
            @ApiResponse(responseCode = "400", description = "VALIDATION_ERROR (storeId missing, store closed)"),
            @ApiResponse(responseCode = "404", description = "ORDER_NOT_FOUND, STORE_NOT_FOUND"),
            @ApiResponse(responseCode = "409", description = "INVALID_ORDER_STATUS (no longer pending)")
    })
    public ResponseEntity<ApiResult<AdminOrderResponse>> reassignStore(
            @Parameter(hidden = true) @AuthenticationPrincipal Jwt jwt,
            @PathVariable Long id,
            @Valid @RequestBody OrderStoreRequest request) {
        return ResponseEntity.ok(ApiResult.ok(adminOrderService.reassignStore(staffId(jwt), id, request)));
    }

    /** The access token subject is the user id (see JwtTokenService). */
    private static Long staffId(Jwt jwt) {
        return Long.valueOf(jwt.getSubject());
    }
}
