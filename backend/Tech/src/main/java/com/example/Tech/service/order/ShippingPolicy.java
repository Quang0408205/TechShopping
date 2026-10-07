package com.example.Tech.service.order;

import com.example.Tech.entity.order.DeliveryType;

import java.math.BigDecimal;

/**
 * Shipping fee rule shared by the cart (preview) and checkout (charged): 30.000đ, free from 10.000.000đ,
 * nothing when there is nothing to pay for. Same rule as the former frontend mock (decision 2026-10-02); store
 * pickup is always free.
 */
public final class ShippingPolicy {

    public static final BigDecimal FLAT_FEE = new BigDecimal("30000");

    public static final BigDecimal FREE_FROM = new BigDecimal("10000000");

    private ShippingPolicy() {
    }

    /** subtotal = sum of the payable lines. Home delivery (the cart preview assumes it). */
    public static BigDecimal feeFor(BigDecimal subtotal) {
        if (subtotal == null || subtotal.signum() <= 0 || subtotal.compareTo(FREE_FROM) >= 0) {
            return BigDecimal.ZERO;
        }
        return FLAT_FEE;
    }

    /** Picking the order up at a store is free (Phase 7). */
    public static BigDecimal feeFor(BigDecimal subtotal, DeliveryType deliveryType) {
        return deliveryType == DeliveryType.PICKUP ? BigDecimal.ZERO : feeFor(subtotal);
    }
}
