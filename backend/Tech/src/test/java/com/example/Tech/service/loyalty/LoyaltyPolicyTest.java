package com.example.Tech.service.loyalty;

import com.example.Tech.entity.loyalty.LoyaltyTier;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class LoyaltyPolicyTest {

    private static final List<LoyaltyTier> TIERS = List.of(
            new LoyaltyTier("GOLD", "Vàng", new BigDecimal("150000000"), new BigDecimal("1.50"), 3),
            new LoyaltyTier("MEMBER", "Thành viên", BigDecimal.ZERO, BigDecimal.ONE, 1),
            new LoyaltyTier("DIAMOND", "Kim cương", new BigDecimal("300000000"), new BigDecimal("2.00"), 4),
            new LoyaltyTier("SILVER", "Bạc", new BigDecimal("50000000"), new BigDecimal("1.20"), 2));

    @Test
    void earned_onePointPer10000_timesTheMultiplier_roundedDown() {
        assertThat(LoyaltyPolicy.earnedPoints(new BigDecimal("15990000"), BigDecimal.ONE)).isEqualTo(1599);
        assertThat(LoyaltyPolicy.earnedPoints(new BigDecimal("15999999"), BigDecimal.ONE)).isEqualTo(1599);
        assertThat(LoyaltyPolicy.earnedPoints(new BigDecimal("15990000"), new BigDecimal("1.20"))).isEqualTo(1918);
        assertThat(LoyaltyPolicy.earnedPoints(new BigDecimal("9999"), new BigDecimal("2.00"))).isZero();
        assertThat(LoyaltyPolicy.earnedPoints(BigDecimal.ZERO, BigDecimal.ONE)).isZero();
    }

    @Test
    void redeem_atMostTenPercentOfTheGoods_in1000dSteps() {
        assertThat(LoyaltyPolicy.maxRedeemable(new BigDecimal("200000"))).isEqualTo(20);
        assertThat(LoyaltyPolicy.maxRedeemable(new BigDecimal("15990000"))).isEqualTo(1599);
        assertThat(LoyaltyPolicy.maxRedeemable(new BigDecimal("9999"))).isZero();
        assertThat(LoyaltyPolicy.valueOf(20)).isEqualByComparingTo("20000");
    }

    @Test
    void tier_isTheHighestThresholdReached() {
        assertThat(LoyaltyPolicy.tierFor(TIERS, BigDecimal.ZERO).getCode()).isEqualTo("MEMBER");
        assertThat(LoyaltyPolicy.tierFor(TIERS, new BigDecimal("49999999")).getCode()).isEqualTo("MEMBER");
        assertThat(LoyaltyPolicy.tierFor(TIERS, new BigDecimal("50000000")).getCode()).isEqualTo("SILVER");
        assertThat(LoyaltyPolicy.tierFor(TIERS, new BigDecimal("299999999")).getCode()).isEqualTo("GOLD");
        assertThat(LoyaltyPolicy.tierFor(TIERS, new BigDecimal("900000000")).getCode()).isEqualTo("DIAMOND");
    }

    @Test
    void revoked_isTheShareOfTheRefund_neverMoreThanEarned() {
        assertThat(LoyaltyPolicy.revokedPoints(1599, new BigDecimal("8000000"), new BigDecimal("15990000"))).isEqualTo(800);
        assertThat(LoyaltyPolicy.revokedPoints(1599, new BigDecimal("99000000"), new BigDecimal("15990000"))).isEqualTo(1599);
        assertThat(LoyaltyPolicy.revokedPoints(0, new BigDecimal("8000000"), new BigDecimal("15990000"))).isZero();
    }

    @Test
    void split_isProportional_andAddsUpExactly() {
        Map<Integer, BigDecimal> lines = new LinkedHashMap<>();
        lines.put(0, new BigDecimal("15990000"));
        lines.put(1, new BigDecimal("200000"));
        lines.put(2, new BigDecimal("100000"));

        Map<Integer, BigDecimal> shares = LoyaltyPolicy.split(lines, new BigDecimal("1000000"));

        assertThat(shares.values().stream().reduce(BigDecimal.ZERO, BigDecimal::add)).isEqualByComparingTo("1000000");
        assertThat(shares.get(1)).isEqualByComparingTo("12277");
        assertThat(shares.get(2)).isEqualByComparingTo("6138");
        assertThat(shares.get(0)).isEqualByComparingTo("981585");
    }
}
