package com.example.Tech.service.inventory;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

class CostPolicyTest {

    @Test
    void noStockOrUnknownCost_theNewPriceIsTheAverage() {
        assertThat(CostPolicy.weightedAverage(0, new BigDecimal("900"), 5, new BigDecimal("1000")))
                .isEqualByComparingTo("1000");
        assertThat(CostPolicy.weightedAverage(10, null, 5, new BigDecimal("1000"))).isEqualByComparingTo("1000");
    }

    @Test
    void existingStock_isWeightedByQuantity() {
        // 10 units at 1.000 + 30 units at 2.000 = 70.000 / 40
        assertThat(CostPolicy.weightedAverage(10, new BigDecimal("1000"), 30, new BigDecimal("2000")))
                .isEqualByComparingTo("1750");
    }

    @Test
    void theResultIsRoundedToTwoDecimals() {
        assertThat(CostPolicy.weightedAverage(1, new BigDecimal("100"), 2, new BigDecimal("100.01")))
                .isEqualByComparingTo("100.01");
        assertThat(CostPolicy.weightedAverage(2, new BigDecimal("100"), 1, new BigDecimal("101")))
                .isEqualByComparingTo("100.33");
    }
}
