package com.example.Tech.controller.payment;

import com.example.Tech.dto.request.payment.InstallmentPartnerUpdateRequest;
import com.example.Tech.dto.response.common.ApiResult;
import com.example.Tech.dto.response.payment.InstallmentPartnerResponse;
import com.example.Tech.service.payment.AdminInstallmentPartnerService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/** Installment partners: conditions and fee tables (ADMIN only: URL rule + @PreAuthorize + a database re-check). */
@RestController
@RequestMapping("/api/v1/admin/installment-partners")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
@Tag(name = "Admin - Installment partners", description = "Order range and fee per term of each partner (ADMIN only)")
@ApiResponses({
        @ApiResponse(responseCode = "401", description = "UNAUTHORIZED, INVALID_TOKEN"),
        @ApiResponse(responseCode = "403", description = "ACCESS_DENIED, ACCOUNT_DISABLED")
})
public class AdminInstallmentPartnerController {

    private final AdminInstallmentPartnerService service;

    @GetMapping
    @Operation(summary = "The installment partners with their conditions and fee tables")
    public ResponseEntity<ApiResult<List<InstallmentPartnerResponse>>> list(@Parameter(hidden = true) @AuthenticationPrincipal Jwt jwt) {
        return ResponseEntity.ok(ApiResult.ok(service.list(Long.valueOf(jwt.getSubject()))));
    }

    @PutMapping("/{code}")
    @Operation(summary = "Save one partner: active flag, order range and the fee of each term (applies to later approvals)")
    @ApiResponses({
            @ApiResponse(responseCode = "400", description = "VALIDATION_ERROR (max below min, a term twice, fee outside 0–100)"),
            @ApiResponse(responseCode = "404", description = "INSTALLMENT_NOT_FOUND")
    })
    public ResponseEntity<ApiResult<InstallmentPartnerResponse>> update(@Parameter(hidden = true) @AuthenticationPrincipal Jwt jwt,
                                                                        @PathVariable String code,
                                                                        @Valid @RequestBody InstallmentPartnerUpdateRequest request) {
        return ResponseEntity.ok(ApiResult.ok(service.update(Long.valueOf(jwt.getSubject()), code, request)));
    }
}
