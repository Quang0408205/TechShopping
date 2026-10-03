package com.example.Tech.service.promotion;

import java.math.BigDecimal;

/**
 * The price actually charged for one unit of a variant, after comparing the manual discount_price
 * column against any currently active promotion and taking whichever is lower.
 */
public record EffectivePrice(BigDecimal unitPrice, BigDecimal originalPrice, String activePromotionName) {

    public boolean hasDiscount() {
        return unitPrice.compareTo(originalPrice) < 0;
    }
}
