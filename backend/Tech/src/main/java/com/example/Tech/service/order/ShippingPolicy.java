package com.example.Tech.service.order;

import com.example.Tech.entity.order.DeliveryType;

import java.math.BigDecimal;

/**
 * Default shipping fee shared by the cart (preview) and checkout (charged): 30.000đ for every home delivery with
 * something to pay for, whatever the order value; a province with its own fee is handled by {@link ShippingFeeService}.
 * Picking the order up at a store is always free.
 */
public final class ShippingPolicy {

    public static final BigDecimal FLAT_FEE = new BigDecimal("30000");

    private ShippingPolicy() {
    }

    /** subtotal = sum of the payable lines. Home delivery (the cart preview assumes it). */
    public static BigDecimal feeFor(BigDecimal subtotal) {
        if (subtotal == null || subtotal.signum() <= 0) {
            return BigDecimal.ZERO;
        }
        return FLAT_FEE;
    }

    /** Picking the order up at a store is free. */
    public static BigDecimal feeFor(BigDecimal subtotal, DeliveryType deliveryType) {
        return deliveryType == DeliveryType.PICKUP ? BigDecimal.ZERO : feeFor(subtotal);
    }
}
