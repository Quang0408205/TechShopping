package com.example.Tech.controller.aftersales;

import com.example.Tech.dto.request.aftersales.AdminServiceRequestSearchRequest;
import com.example.Tech.dto.request.aftersales.ServiceRequestUpdateRequest;
import com.example.Tech.dto.response.aftersales.ServiceRequestResponse;
import com.example.Tech.dto.response.common.ApiResult;
import com.example.Tech.dto.response.common.PageResponse;
import com.example.Tech.entity.aftersales.ServiceRequestType;
import com.example.Tech.service.aftersales.AdminServiceRequestService;
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
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** STAFF (own store only) and ADMIN (all stores) handle warranty / maintenance / return requests. */
@RestController
@RequestMapping("/api/v1/admin/service-requests")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('STAFF', 'BRANCH_MANAGER', 'ADMIN')")
@Tag(name = "Admin - After-sales", description = "Warranty, maintenance and return requests (STAFF, ADMIN)")
@ApiResponses({
        @ApiResponse(responseCode = "401", description = "UNAUTHORIZED, INVALID_TOKEN"),
        @ApiResponse(responseCode = "403", description = "ACCESS_DENIED, ACCOUNT_DISABLED, NO_ACTIVE_STORE_ASSIGNMENT")
})
public class AdminServiceRequestController {

    private final AdminServiceRequestService adminServiceRequestService;

    @GetMapping
    @Operation(summary = "Requests of the three types, newest first; a STAFF member sees their store only")
    @ApiResponse(responseCode = "200", description = "Page of requests")
    public ResponseEntity<ApiResult<PageResponse<ServiceRequestResponse>>> search(
            @Parameter(hidden = true) @AuthenticationPrincipal Jwt jwt,
            @Valid @ParameterObject AdminServiceRequestSearchRequest filter,
            @ParameterObject @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC)
            Pageable pageable) {
        return ResponseEntity.ok(ApiResult.ok(adminServiceRequestService.search(userId(jwt), filter, pageable)));
    }

    @GetMapping("/{type}/{id}")
    @Operation(summary = "One request")
    @ApiResponse(responseCode = "404", description = "SERVICE_REQUEST_NOT_FOUND")
    public ResponseEntity<ApiResult<ServiceRequestResponse>> get(
            @Parameter(hidden = true) @AuthenticationPrincipal Jwt jwt,
            @PathVariable ServiceRequestType type, @PathVariable Long id) {
        return ResponseEntity.ok(ApiResult.ok(adminServiceRequestService.get(userId(jwt), type, id)));
    }

    @PatchMapping("/{type}/{id}")
    @Operation(summary = "Move a request to its next status and / or update its notes, dates and costs",
            description = "Warranty / maintenance: PENDING → RECEIVED → PROCESSING → COMPLETED, REJECTED (reason) from the "
                    + "open ones. Return: PENDING → APPROVED → RECEIVED (restockOrderItemIds go back into the store's "
                    + "stock) → REFUNDED (the refund is taken off the customer's total spent), REJECTED (reason) from "
                    + "PENDING / APPROVED.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "The request after the change"),
            @ApiResponse(responseCode = "400", description = "VALIDATION_ERROR (details per field)"),
            @ApiResponse(responseCode = "404", description = "SERVICE_REQUEST_NOT_FOUND"),
            @ApiResponse(responseCode = "409", description = "INVALID_SERVICE_REQUEST_STATUS")
    })
    public ResponseEntity<ApiResult<ServiceRequestResponse>> update(
            @Parameter(hidden = true) @AuthenticationPrincipal Jwt jwt,
            @PathVariable ServiceRequestType type, @PathVariable Long id,
            @Valid @RequestBody ServiceRequestUpdateRequest request) {
        return ResponseEntity.ok(ApiResult.ok(adminServiceRequestService.update(userId(jwt), type, id, request)));
    }

    private static Long userId(Jwt jwt) {
        return Long.valueOf(jwt.getSubject());
    }
}
