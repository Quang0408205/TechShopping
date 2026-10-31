package com.example.Tech.controller.order;

import com.example.Tech.dto.request.order.ShippingFeeRuleRequest;
import com.example.Tech.dto.response.common.ApiResult;
import com.example.Tech.dto.response.order.ShippingFeeRuleResponse;
import com.example.Tech.service.order.ShippingFeeService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/** Shipping fee of the provinces that have their own. ADMIN only (URL rule, @PreAuthorize and the service). */
@RestController
@RequestMapping("/api/v1/admin/shipping-fees")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
@Tag(name = "Admin - Shipping fees", description = "Province-specific home delivery fees; the others pay the default fee")
@ApiResponse(responseCode = "401", description = "UNAUTHORIZED, INVALID_TOKEN")
@ApiResponse(responseCode = "403", description = "ACCESS_DENIED")
public class AdminShippingFeeController {

    private final ShippingFeeService shippingFeeService;

    @GetMapping
    @Operation(summary = "Provinces with their own fee, by name")
    public ResponseEntity<ApiResult<List<ShippingFeeRuleResponse>>> list(@Parameter(hidden = true) @AuthenticationPrincipal Jwt jwt) {
        return ResponseEntity.ok(ApiResult.ok(shippingFeeService.list(Long.valueOf(jwt.getSubject()))));
    }

    @PostMapping
    @Operation(summary = "Give a province its own fee")
    @ApiResponse(responseCode = "409", description = "DUPLICATE_SHIPPING_FEE_RULE")
    public ResponseEntity<ApiResult<ShippingFeeRuleResponse>> create(@Parameter(hidden = true) @AuthenticationPrincipal Jwt jwt,
                                                                      @Valid @RequestBody ShippingFeeRuleRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResult.ok(shippingFeeService.create(Long.valueOf(jwt.getSubject()), request)));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Change a province's fee")
    @ApiResponse(responseCode = "404", description = "SHIPPING_FEE_RULE_NOT_FOUND")
    @ApiResponse(responseCode = "409", description = "DUPLICATE_SHIPPING_FEE_RULE")
    public ResponseEntity<ApiResult<ShippingFeeRuleResponse>> update(@Parameter(hidden = true) @AuthenticationPrincipal Jwt jwt,
                                                                      @PathVariable Integer id,
                                                                      @Valid @RequestBody ShippingFeeRuleRequest request) {
        return ResponseEntity.ok(ApiResult.ok(shippingFeeService.update(Long.valueOf(jwt.getSubject()), id, request)));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Back to the default fee for that province")
    @ApiResponse(responseCode = "404", description = "SHIPPING_FEE_RULE_NOT_FOUND")
    public ResponseEntity<ApiResult<Void>> delete(@Parameter(hidden = true) @AuthenticationPrincipal Jwt jwt, @PathVariable Integer id) {
        shippingFeeService.delete(Long.valueOf(jwt.getSubject()), id);
        return ResponseEntity.ok(ApiResult.ok(null));
    }
}
