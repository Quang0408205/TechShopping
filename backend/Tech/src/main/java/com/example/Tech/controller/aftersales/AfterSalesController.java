package com.example.Tech.controller.aftersales;

import com.example.Tech.dto.request.aftersales.MaintenanceRequestCreateRequest;
import com.example.Tech.dto.request.aftersales.ReturnRequestCreateRequest;
import com.example.Tech.dto.request.aftersales.ServiceRequestSearchRequest;
import com.example.Tech.dto.request.aftersales.WarrantyRequestCreateRequest;
import com.example.Tech.dto.response.aftersales.AfterSalesOrderResponse;
import com.example.Tech.dto.response.aftersales.ServiceRequestResponse;
import com.example.Tech.dto.response.common.ApiResult;
import com.example.Tech.dto.response.common.PageResponse;
import com.example.Tech.entity.aftersales.ServiceRequestType;
import com.example.Tech.service.aftersales.AfterSalesService;
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
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** The caller's own after-sales requests; another account's request or order answers 404. */
@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
@Tag(name = "After-sales", description = "Warranty, maintenance and return requests of the logged-in customer")
@ApiResponses({
        @ApiResponse(responseCode = "401", description = "UNAUTHORIZED, INVALID_TOKEN"),
        @ApiResponse(responseCode = "403", description = "ACCOUNT_DISABLED")
})
public class AfterSalesController {

    private final AfterSalesService afterSalesService;

    @GetMapping("/orders/{id}/after-sales")
    @Operation(summary = "Warranty per line, open requests, returnable quantities and the return deadline of a delivered order")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "After-sales view of the order"),
            @ApiResponse(responseCode = "404", description = "ORDER_NOT_FOUND"),
            @ApiResponse(responseCode = "409", description = "AFTER_SALES_NOT_AVAILABLE (not delivered)")
    })
    public ResponseEntity<ApiResult<AfterSalesOrderResponse>> orderAfterSales(
            @Parameter(hidden = true) @AuthenticationPrincipal Jwt jwt, @PathVariable Long id) {
        return ResponseEntity.ok(ApiResult.ok(afterSalesService.orderAfterSales(userId(jwt), id)));
    }

    @PostMapping("/warranty-requests")
    @Operation(summary = "Ask for a warranty repair of a line still under warranty (10–2000 characters, up to 5 photos)")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Request created (PENDING)"),
            @ApiResponse(responseCode = "400", description = "VALIDATION_ERROR (details per field)"),
            @ApiResponse(responseCode = "404", description = "ORDER_NOT_FOUND"),
            @ApiResponse(responseCode = "409", description = "AFTER_SALES_NOT_AVAILABLE, WARRANTY_NOT_VALID, SERVICE_REQUEST_ALREADY_OPEN")
    })
    public ResponseEntity<ApiResult<ServiceRequestResponse>> createWarrantyRequest(
            @Parameter(hidden = true) @AuthenticationPrincipal Jwt jwt,
            @Valid @RequestBody WarrantyRequestCreateRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResult.ok(afterSalesService.createWarrantyRequest(userId(jwt), request)));
    }

    @PostMapping("/maintenance-requests")
    @Operation(summary = "Ask for maintenance of any bought line (no warranty needed; costs are paid at the store)")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Request created (PENDING)"),
            @ApiResponse(responseCode = "400", description = "VALIDATION_ERROR"),
            @ApiResponse(responseCode = "404", description = "ORDER_NOT_FOUND"),
            @ApiResponse(responseCode = "409", description = "AFTER_SALES_NOT_AVAILABLE, SERVICE_REQUEST_ALREADY_OPEN")
    })
    public ResponseEntity<ApiResult<ServiceRequestResponse>> createMaintenanceRequest(
            @Parameter(hidden = true) @AuthenticationPrincipal Jwt jwt,
            @Valid @RequestBody MaintenanceRequestCreateRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResult.ok(afterSalesService.createMaintenanceRequest(userId(jwt), request)));
    }

    @PostMapping("/return-requests")
    @Operation(summary = "Return lines of a delivered order for a refund, within 7 days of delivery (not installment orders)")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Request created (PENDING) with the computed refund"),
            @ApiResponse(responseCode = "400", description = "VALIDATION_ERROR (details.items for lines / quantities)"),
            @ApiResponse(responseCode = "404", description = "ORDER_NOT_FOUND"),
            @ApiResponse(responseCode = "409", description = "AFTER_SALES_NOT_AVAILABLE, RETURN_NOT_AVAILABLE")
    })
    public ResponseEntity<ApiResult<ServiceRequestResponse>> createReturnRequest(
            @Parameter(hidden = true) @AuthenticationPrincipal Jwt jwt,
            @Valid @RequestBody ReturnRequestCreateRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResult.ok(afterSalesService.createReturnRequest(userId(jwt), request)));
    }

    @GetMapping("/service-requests/mine")
    @Operation(summary = "My requests of the three types, newest first")
    @ApiResponse(responseCode = "200", description = "Page of requests")
    public ResponseEntity<ApiResult<PageResponse<ServiceRequestResponse>>> mine(
            @Parameter(hidden = true) @AuthenticationPrincipal Jwt jwt,
            @ParameterObject ServiceRequestSearchRequest filter,
            @ParameterObject @PageableDefault(size = 10, sort = "createdAt", direction = Sort.Direction.DESC)
            Pageable pageable) {
        return ResponseEntity.ok(ApiResult.ok(afterSalesService.mine(userId(jwt), filter, pageable)));
    }

    @GetMapping("/service-requests/{type}/{id}")
    @Operation(summary = "One of my requests")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "The request"),
            @ApiResponse(responseCode = "404", description = "SERVICE_REQUEST_NOT_FOUND")
    })
    public ResponseEntity<ApiResult<ServiceRequestResponse>> getMine(
            @Parameter(hidden = true) @AuthenticationPrincipal Jwt jwt,
            @PathVariable ServiceRequestType type, @PathVariable Long id) {
        return ResponseEntity.ok(ApiResult.ok(afterSalesService.getMine(userId(jwt), type, id)));
    }

    @PostMapping("/service-requests/{type}/{id}/cancel")
    @Operation(summary = "Cancel one of my requests while it is still PENDING")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "The request, now CANCELLED"),
            @ApiResponse(responseCode = "404", description = "SERVICE_REQUEST_NOT_FOUND"),
            @ApiResponse(responseCode = "409", description = "INVALID_SERVICE_REQUEST_STATUS")
    })
    public ResponseEntity<ApiResult<ServiceRequestResponse>> cancel(
            @Parameter(hidden = true) @AuthenticationPrincipal Jwt jwt,
            @PathVariable ServiceRequestType type, @PathVariable Long id) {
        return ResponseEntity.ok(ApiResult.ok(afterSalesService.cancel(userId(jwt), type, id)));
    }

    private static Long userId(Jwt jwt) {
        return Long.valueOf(jwt.getSubject());
    }
}
