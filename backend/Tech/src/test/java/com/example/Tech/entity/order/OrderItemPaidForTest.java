package com.example.Tech.entity.order;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

class OrderItemPaidForTest {

    @Test
    void aRefundPaysBackWhatWasPaid_afterComboVoucherAndPoints() {
        OrderItem item = new OrderItem();
        item.setQuantity(2);
        item.setUnitPrice(new BigDecimal("200000"));
        item.setSubtotal(new BigDecimal("400000"));
        item.setComboDiscount(new BigDecimal("100000"));
        item.setVoucherDiscount(new BigDecimal("30000"));
        item.setPointsDiscount(new BigDecimal("10000"));

        assertThat(item.afterCombo()).isEqualByComparingTo("300000");
        assertThat(item.paidFor(2)).isEqualByComparingTo("260000");
        assertThat(item.paidFor(1)).isEqualByComparingTo("130000");
    }
}
