package com.example.Tech.entity.promotion;

import java.time.LocalDateTime;

/**
 * Display status of a promotion, derived from is_active and the date range (not stored). An ended
 * promotion is ENDED even when it was paused. PromotionFilterSpecifications applies the same rules in SQL.
 */
public enum PromotionStatus {

    UPCOMING,
    RUNNING,
    PAUSED,
    ENDED;

    public static PromotionStatus of(Promotion promotion, LocalDateTime now) {
        if (!now.isBefore(promotion.getEndDate())) {
            return ENDED;
        }
        if (!Boolean.TRUE.equals(promotion.getActive())) {
            return PAUSED;
        }
        return now.isBefore(promotion.getStartDate()) ? UPCOMING : RUNNING;
    }
}
