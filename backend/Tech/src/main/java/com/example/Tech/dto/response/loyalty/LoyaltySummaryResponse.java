package com.example.Tech.dto.response.loyalty;

import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.util.List;

/** The customer's points, tier and progress (plan v5 GĐ6). */
public record LoyaltySummaryResponse(
        int points,
        @Schema(description = "What the points are worth at checkout (1 point = 1.000đ)")
        BigDecimal pointsValue,
        LoyaltyTierResponse tier,
        @Schema(description = "Next tier, null at the top")
        LoyaltyTierResponse nextTier,
        @Schema(description = "Total spent on delivered orders (returns refunded are taken off)")
        BigDecimal totalSpent,
        @Schema(description = "Still to spend to reach the next tier (0 at the top)")
        BigDecimal amountToNextTier,
        @Schema(description = "0–100: progress from the current tier to the next one")
        int progressPercent,
        List<LoyaltyTierResponse> tiers,
        Rules rules
) {

    public record Rules(BigDecimal vndPerPointEarned, BigDecimal vndPerPointSpent, int maxRedeemPercent) {
    }
}
