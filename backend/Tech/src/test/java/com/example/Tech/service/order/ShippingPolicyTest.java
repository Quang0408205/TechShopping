package com.example.Tech.service.order;

import com.example.Tech.entity.order.DeliveryType;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

class ShippingPolicyTest {

    @Test
    void feeFor_flatFeeBelowTheThreshold_freeFromIt_zeroWithoutAnythingToPay() {
        assertThat(ShippingPolicy.feeFor(new BigDecimal("1"))).isEqualByComparingTo("30000");
        assertThat(ShippingPolicy.feeFor(new BigDecimal("9999999.99"))).isEqualByComparingTo("30000");
        assertThat(ShippingPolicy.feeFor(new BigDecimal("10000000"))).isEqualByComparingTo("0");
        assertThat(ShippingPolicy.feeFor(new BigDecimal("45000000"))).isEqualByComparingTo("0");
        assertThat(ShippingPolicy.feeFor(BigDecimal.ZERO)).isEqualByComparingTo("0");
        assertThat(ShippingPolicy.feeFor(null)).isEqualByComparingTo("0");
    }

    @Test
    void feeFor_pickupIsAlwaysFree_homeDeliveryUsesTheNormalRule() {
        assertThat(ShippingPolicy.feeFor(new BigDecimal("200000"), DeliveryType.PICKUP)).isEqualByComparingTo("0");
        assertThat(ShippingPolicy.feeFor(new BigDecimal("200000"), DeliveryType.HOME_DELIVERY))
                .isEqualByComparingTo("30000");
    }
}
