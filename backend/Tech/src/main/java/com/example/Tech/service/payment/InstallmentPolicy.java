package com.example.Tech.service.payment;

import java.math.BigDecimal;
import java.math.RoundingMode;

/** Installment rules: 0% interest for the customer and no down payment; the citizen id has exactly 10 digits. */
public final class InstallmentPolicy {

    public static final BigDecimal MIN_ORDER_TOTAL = new BigDecimal("3000000");
    /** The user asked for exactly 10 digits (a real CCCD has 12): change here and in the frontend together. */
    public static final int CITIZEN_ID_LENGTH = 10;

    private InstallmentPolicy() {
    }

    /** total / months rounded down to the whole đồng; the last period takes the remainder. */
    public static BigDecimal monthlyPayment(BigDecimal total, int months) {
        return total.divide(BigDecimal.valueOf(months), 0, RoundingMode.DOWN);
    }

    public static BigDecimal lastPayment(BigDecimal total, int months) {
        return total.subtract(monthlyPayment(total, months).multiply(BigDecimal.valueOf(months - 1L)));
    }

    /** Only the last 4 digits stay visible, e.g. ******6789. */
    public static String maskCitizenId(String citizenId) {
        if (citizenId == null || citizenId.length() <= 4) {
            return citizenId;
        }
        return "*".repeat(citizenId.length() - 4) + citizenId.substring(citizenId.length() - 4);
    }
}
