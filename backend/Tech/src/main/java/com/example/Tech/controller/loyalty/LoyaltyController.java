package com.example.Tech.controller.loyalty;

import com.example.Tech.dto.response.common.ApiResult;
import com.example.Tech.dto.response.common.PageResponse;
import com.example.Tech.dto.response.loyalty.LoyaltySummaryResponse;
import com.example.Tech.dto.response.loyalty.LoyaltyTransactionResponse;
import com.example.Tech.service.loyalty.LoyaltyService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Plan v5 GĐ6: my points and tier. Requires a valid access token. */
@RestController
@RequestMapping("/api/v1/loyalty")
@RequiredArgsConstructor
@Tag(name = "Loyalty", description = "Loyalty points and membership tiers of the logged-in customer")
@ApiResponse(responseCode = "401", description = "UNAUTHORIZED, INVALID_TOKEN")
public class LoyaltyController {

    private final LoyaltyService loyaltyService;

    @GetMapping("/me")
    @Operation(summary = "My points, tier, progress to the next tier, all tiers and the point rules")
    public ResponseEntity<ApiResult<LoyaltySummaryResponse>> summary(@Parameter(hidden = true) @AuthenticationPrincipal Jwt jwt) {
        return ResponseEntity.ok(ApiResult.ok(loyaltyService.summary(Long.valueOf(jwt.getSubject()))));
    }

    @GetMapping("/me/history")
    @Operation(summary = "My points history, newest first")
    public ResponseEntity<ApiResult<PageResponse<LoyaltyTransactionResponse>>> history(
            @Parameter(hidden = true) @AuthenticationPrincipal Jwt jwt,
            @ParameterObject @PageableDefault(size = 10) Pageable pageable) {
        return ResponseEntity.ok(ApiResult.ok(loyaltyService.history(Long.valueOf(jwt.getSubject()), pageable)));
    }
}
