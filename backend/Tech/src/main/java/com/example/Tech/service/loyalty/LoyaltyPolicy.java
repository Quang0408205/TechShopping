package com.example.Tech.service.loyalty;

import com.example.Tech.entity.loyalty.LoyaltyTier;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Plan v5 GĐ6 point rules (pure functions). Earn 1 point per {@link #VND_PER_POINT_EARNED} of goods really paid
 * (after promotions, voucher and points, shipping excluded) × the tier multiplier, rounded down. Spend: 1 point =
 * {@link #VND_PER_POINT_SPENT}, at most {@link #MAX_REDEEM_PERCENT}% of the goods after the voucher.
 */
public final class LoyaltyPolicy {

    public static final BigDecimal VND_PER_POINT_EARNED = BigDecimal.valueOf(10_000);
    public static final BigDecimal VND_PER_POINT_SPENT = BigDecimal.valueOf(1_000);
    public static final int MAX_REDEEM_PERCENT = 10;

    private LoyaltyPolicy() {
    }

    public static int earnedPoints(BigDecimal paidGoods, BigDecimal multiplier) {
        if (paidGoods == null || paidGoods.signum() <= 0) {
            return 0;
        }
        BigDecimal basePoints = paidGoods.divide(VND_PER_POINT_EARNED, 0, RoundingMode.FLOOR);
        return basePoints.multiply(multiplier).setScale(0, RoundingMode.FLOOR).intValueExact();
    }

    /** Most points one order may spend: 10% of the goods after the voucher, in whole points. */
    public static int maxRedeemable(BigDecimal goodsAfterVoucher) {
        if (goodsAfterVoucher == null || goodsAfterVoucher.signum() <= 0) {
            return 0;
        }
        return goodsAfterVoucher.multiply(BigDecimal.valueOf(MAX_REDEEM_PERCENT))
                .divide(BigDecimal.valueOf(100).multiply(VND_PER_POINT_SPENT), 0, RoundingMode.FLOOR)
                .intValueExact();
    }

    public static BigDecimal valueOf(int points) {
        return VND_PER_POINT_SPENT.multiply(BigDecimal.valueOf(points));
    }

    /** Highest tier whose threshold the spending reaches ({@code tiers} in any order, one with threshold 0). */
    public static LoyaltyTier tierFor(List<LoyaltyTier> tiers, BigDecimal spent) {
        BigDecimal amount = spent == null ? BigDecimal.ZERO : spent;
        return tiers.stream()
                .filter(tier -> tier.getMinSpent().compareTo(amount) <= 0)
                .max(Comparator.comparingInt(LoyaltyTier::getSortOrder))
                .orElseGet(() -> tiers.stream().min(Comparator.comparingInt(LoyaltyTier::getSortOrder)).orElseThrow());
    }

    /** Points earned on an order taken back when {@code refund} of its {@code paidGoods} is refunded. */
    public static int revokedPoints(int pointsEarned, BigDecimal refund, BigDecimal paidGoods) {
        if (pointsEarned <= 0 || refund == null || refund.signum() <= 0 || paidGoods == null || paidGoods.signum() <= 0) {
            return 0;
        }
        BigDecimal share = BigDecimal.valueOf(pointsEarned).multiply(refund.min(paidGoods))
                .divide(paidGoods, 0, RoundingMode.FLOOR);
        return share.intValueExact();
    }

    /**
     * Splits {@code discount} over lines in proportion to their amounts (keys kept); rounding left-overs go to the
     * largest line, so the shares add up exactly.
     */
    public static <K> Map<K, BigDecimal> split(Map<K, BigDecimal> amounts, BigDecimal discount) {
        Map<K, BigDecimal> shares = new LinkedHashMap<>();
        BigDecimal total = amounts.values().stream().reduce(BigDecimal.ZERO, BigDecimal::add);
        if (total.signum() <= 0) {
            return shares;
        }
        BigDecimal given = BigDecimal.ZERO;
        K largest = null;
        for (Map.Entry<K, BigDecimal> line : amounts.entrySet()) {
            BigDecimal share = discount.multiply(line.getValue()).divide(total, 0, RoundingMode.FLOOR);
            shares.put(line.getKey(), share);
            given = given.add(share);
            if (largest == null || line.getValue().compareTo(amounts.get(largest)) > 0) {
                largest = line.getKey();
            }
        }
        shares.merge(largest, discount.subtract(given), BigDecimal::add);
        return shares;
    }
}
