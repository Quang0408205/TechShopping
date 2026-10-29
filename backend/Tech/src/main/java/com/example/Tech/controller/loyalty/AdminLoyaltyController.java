package com.example.Tech.controller.loyalty;

import com.example.Tech.dto.request.loyalty.LoyaltyTiersUpdateRequest;
import com.example.Tech.dto.response.common.ApiResult;
import com.example.Tech.dto.response.loyalty.LoyaltyTierResponse;
import com.example.Tech.service.loyalty.AdminLoyaltyService;
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
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/** Plan v5 GĐ6: membership tiers (ADMIN only: URL rule + @PreAuthorize + a database re-check). */
@RestController
@RequestMapping("/api/v1/admin/loyalty/tiers")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
@Tag(name = "Admin - Loyalty", description = "Membership tiers: thresholds and point multipliers (ADMIN only)")
@ApiResponses({
        @ApiResponse(responseCode = "401", description = "UNAUTHORIZED, INVALID_TOKEN"),
        @ApiResponse(responseCode = "403", description = "ACCESS_DENIED, ACCOUNT_DISABLED")
})
public class AdminLoyaltyController {

    private final AdminLoyaltyService adminLoyaltyService;

    @GetMapping
    @Operation(summary = "The 4 tiers, lowest first")
    public ResponseEntity<ApiResult<List<LoyaltyTierResponse>>> tiers(@Parameter(hidden = true) @AuthenticationPrincipal Jwt jwt) {
        return ResponseEntity.ok(ApiResult.ok(adminLoyaltyService.tiers(Long.valueOf(jwt.getSubject()))));
    }

    @PutMapping
    @Operation(summary = "Save thresholds + multipliers of all tiers; customers that now qualify move up (never down)")
    @ApiResponse(responseCode = "400", description = "VALIDATION_ERROR (missing tier, lowest not 0, thresholds not rising, multiplier outside 1–5)")
    public ResponseEntity<ApiResult<List<LoyaltyTierResponse>>> update(@Parameter(hidden = true) @AuthenticationPrincipal Jwt jwt,
                                                                       @Valid @RequestBody LoyaltyTiersUpdateRequest request) {
        return ResponseEntity.ok(ApiResult.ok(adminLoyaltyService.update(Long.valueOf(jwt.getSubject()), request)));
    }
}
