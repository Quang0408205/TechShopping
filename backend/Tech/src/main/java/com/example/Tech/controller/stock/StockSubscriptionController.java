package com.example.Tech.controller.stock;

import com.example.Tech.dto.request.stock.StockSubscribeRequest;
import com.example.Tech.dto.response.common.ApiResult;
import com.example.Tech.dto.response.stock.StockAlertRunResponse;
import com.example.Tech.service.stock.StockSubscriptionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
@Tag(name = "Back in stock", description = "Ask for an email when an out-of-stock variant is available again")
@ApiResponse(responseCode = "401", description = "UNAUTHORIZED, INVALID_TOKEN")
public class StockSubscriptionController {

    private final StockSubscriptionService subscriptionService;

    @GetMapping("/stock-subscriptions")
    @Operation(summary = "Variant ids the caller waits for")
    public ResponseEntity<ApiResult<List<Long>>> mine(@Parameter(hidden = true) @AuthenticationPrincipal Jwt jwt) {
        return ResponseEntity.ok(ApiResult.ok(subscriptionService.mine(userId(jwt))));
    }

    @PostMapping("/stock-subscriptions")
    @Operation(summary = "Wait for a variant that is out of stock (idempotent; at most 50 per person)")
    @ApiResponse(responseCode = "404", description = "PRODUCT_VARIANT_NOT_FOUND")
    @ApiResponse(responseCode = "409", description = "VARIANT_IN_STOCK, STOCK_SUBSCRIPTION_LIMIT")
    public ResponseEntity<ApiResult<Void>> subscribe(@Parameter(hidden = true) @AuthenticationPrincipal Jwt jwt,
                                                     @Valid @RequestBody StockSubscribeRequest request) {
        subscriptionService.subscribe(userId(jwt), request.variantId());
        return ResponseEntity.ok(ApiResult.ok(null));
    }

    @DeleteMapping("/stock-subscriptions/{variantId}")
    @Operation(summary = "Stop waiting for a variant")
    public ResponseEntity<ApiResult<Void>> unsubscribe(@Parameter(hidden = true) @AuthenticationPrincipal Jwt jwt,
                                                       @PathVariable Long variantId) {
        subscriptionService.unsubscribe(userId(jwt), variantId);
        return ResponseEntity.ok(ApiResult.ok(null));
    }

    @PostMapping("/admin/stock-alerts/run")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "ADMIN: run the back-in-stock check now (it also runs every 10 minutes)")
    @ApiResponse(responseCode = "403", description = "ACCESS_DENIED (not ADMIN)")
    public ResponseEntity<ApiResult<StockAlertRunResponse>> run() {
        return ResponseEntity.ok(ApiResult.ok(subscriptionService.run()));
    }

    private static Long userId(Jwt jwt) {
        return Long.valueOf(jwt.getSubject());
    }
}
