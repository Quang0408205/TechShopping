package com.example.Tech.service.inventory;

import java.math.BigDecimal;
import java.math.RoundingMode;

/** Weighted average cost of a variant over all stores. */
public final class CostPolicy {

    private CostPolicy() {
    }

    /**
     * The average cost after receiving {@code quantity} units at {@code unitCost} on top of {@code stockBefore} units
     * valued at {@code averageBefore}. With no stock or an unknown previous cost the new price is the average.
     */
    public static BigDecimal weightedAverage(long stockBefore, BigDecimal averageBefore, int quantity, BigDecimal unitCost) {
        if (stockBefore <= 0 || averageBefore == null) {
            return unitCost.setScale(2, RoundingMode.HALF_UP);
        }
        BigDecimal value = averageBefore.multiply(BigDecimal.valueOf(stockBefore))
                .add(unitCost.multiply(BigDecimal.valueOf(quantity)));
        return value.divide(BigDecimal.valueOf(stockBefore + quantity), 2, RoundingMode.HALF_UP);
    }
}
