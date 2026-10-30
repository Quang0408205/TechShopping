package com.example.Tech.controller.voucher;

import com.example.Tech.dto.request.voucher.VoucherApplyRequest;
import com.example.Tech.dto.response.common.ApiResult;
import com.example.Tech.dto.response.voucher.VoucherOptionResponse;
import com.example.Tech.service.voucher.VoucherService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/** Vouchers at checkout, evaluated against the logged-in customer's server cart. */
@RestController
@RequestMapping("/api/v1/vouchers")
@RequiredArgsConstructor
@Tag(name = "Vouchers", description = "Order vouchers the customer can use at checkout")
@ApiResponses({
        @ApiResponse(responseCode = "401", description = "UNAUTHORIZED, INVALID_TOKEN"),
        @ApiResponse(responseCode = "403", description = "ACCOUNT_DISABLED")
})
public class VoucherController {

    private final VoucherService voucherService;

    @GetMapping("/available")
    @Operation(summary = "Vouchers running now, each evaluated on my cart (usable ones first)")
    public ResponseEntity<ApiResult<List<VoucherOptionResponse>>> available(
            @Parameter(hidden = true) @AuthenticationPrincipal Jwt jwt) {
        return ResponseEntity.ok(ApiResult.ok(voucherService.available(Long.valueOf(jwt.getSubject()))));
    }

    @PostMapping("/apply")
    @Operation(summary = "Check a typed code on my cart (nothing is reserved; the order re-checks it)")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Applicable: discount on the current cart"),
            @ApiResponse(responseCode = "400", description = "VALIDATION_ERROR, VOUCHER_NOT_APPLICABLE (details.reason, details.shortfall)"),
            @ApiResponse(responseCode = "404", description = "VOUCHER_NOT_FOUND")
    })
    public ResponseEntity<ApiResult<VoucherOptionResponse>> apply(
            @Parameter(hidden = true) @AuthenticationPrincipal Jwt jwt, @Valid @RequestBody VoucherApplyRequest request) {
        return ResponseEntity.ok(ApiResult.ok(voucherService.preview(Long.valueOf(jwt.getSubject()), request.code())));
    }
}
