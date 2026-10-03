package com.example.Tech.controller.payment;

import com.example.Tech.dto.request.payment.AdminInstallmentSearchRequest;
import com.example.Tech.dto.request.payment.PaymentConfirmRequest;
import com.example.Tech.dto.response.common.ApiResult;
import com.example.Tech.dto.response.common.PageResponse;
import com.example.Tech.dto.response.payment.AdminInstallmentResponse;
import com.example.Tech.service.payment.AdminInstallmentService;
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
 * Installment plans for staff. Protected by URL (/api/v1/admin/installments/** = STAFF or ADMIN in SecurityConfig)
 * and by @PreAuthorize; the service also re-checks the caller's roles in the database.
 */
@RestController
@RequestMapping("/api/v1/admin/installments")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('STAFF', 'ADMIN')")
@Tag(name = "Admin - Installments", description = "Installment plans and their periods (STAFF and ADMIN)")
@ApiResponses({
        @ApiResponse(responseCode = "401", description = "UNAUTHORIZED, INVALID_TOKEN"),
        @ApiResponse(responseCode = "403", description = "ACCESS_DENIED, ACCOUNT_DISABLED")
})
public class AdminInstallmentController {

    private final AdminInstallmentService adminInstallmentService;

    @GetMapping
    @Operation(summary = "Search installment plans, newest first by default",
            description = "Filters are optional and combined with AND")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Page of plans with order, customer and next period"),
            @ApiResponse(responseCode = "400", description = "VALIDATION_ERROR / MALFORMED_REQUEST")
    })
    public ResponseEntity<ApiResult<PageResponse<AdminInstallmentResponse>>> search(
            @Parameter(hidden = true) @AuthenticationPrincipal Jwt jwt,
            @Valid @ParameterObject AdminInstallmentSearchRequest filter,
            @ParameterObject @PageableDefault(size = 20, sort = "id", direction = Sort.Direction.DESC) Pageable pageable) {
        return ResponseEntity.ok(ApiResult.ok(adminInstallmentService.search(staffId(jwt), filter, pageable)));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get an installment plan")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Plan found"),
            @ApiResponse(responseCode = "404", description = "INSTALLMENT_NOT_FOUND")
    })
    public ResponseEntity<ApiResult<AdminInstallmentResponse>> getById(
            @Parameter(hidden = true) @AuthenticationPrincipal Jwt jwt,
            @PathVariable Long id) {
        return ResponseEntity.ok(ApiResult.ok(adminInstallmentService.getById(staffId(jwt), id)));
    }

    @PostMapping("/{id}/periods/{number}/pay")
    @Operation(summary = "Record period {number} as paid in full (must be the earliest unpaid one)")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Period recorded; the last one completes the plan"),
            @ApiResponse(responseCode = "400", description = "VALIDATION_ERROR (transactionId > 100)"),
            @ApiResponse(responseCode = "404", description = "INSTALLMENT_NOT_FOUND"),
            @ApiResponse(responseCode = "409", description = "INVALID_INSTALLMENT_STATUS (plan not ACTIVE), "
                    + "INSTALLMENT_PERIOD_OUT_OF_ORDER")
    })
    public ResponseEntity<ApiResult<AdminInstallmentResponse>> payPeriod(
            @Parameter(hidden = true) @AuthenticationPrincipal Jwt jwt,
            @PathVariable Long id,
            @PathVariable int number,
            @Valid @RequestBody(required = false) PaymentConfirmRequest request) {
        return ResponseEntity.ok(ApiResult.ok(adminInstallmentService.payPeriod(staffId(jwt), id, number, request)));
    }

    /** The access token subject is the user id (see JwtTokenService). */
    private static Long staffId(Jwt jwt) {
        return Long.valueOf(jwt.getSubject());
    }
}
