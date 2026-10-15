package com.example.Tech.service.payment;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/** Installment rules (decisions 2026-10-03): 3/6/9/12 months, 0% interest, no down payment, order total ≥ 3.000.000đ. */
public final class InstallmentPolicy {

    public static final List<Integer> TERMS_IN_MONTHS = List.of(3, 6, 9, 12);
    public static final BigDecimal MIN_ORDER_TOTAL = new BigDecimal("3000000");
    /** The user asked for exactly 10 digits (a real CCCD has 12): change here and in the frontend together. */
    public static final int CITIZEN_ID_LENGTH = 10;

    private InstallmentPolicy() {
    }

    public static boolean isAllowedTerm(Integer months) {
        return months != null && TERMS_IN_MONTHS.contains(months);
    }

    public static boolean isEligible(BigDecimal orderTotal) {
        return orderTotal != null && orderTotal.compareTo(MIN_ORDER_TOTAL) >= 0;
    }

    /** total / months rounded down to the whole đồng; the last period takes the remainder. */
    public static BigDecimal monthlyPayment(BigDecimal total, int months) {
        return total.divide(BigDecimal.valueOf(months), 0, RoundingMode.DOWN);
    }

    public static BigDecimal lastPayment(BigDecimal total, int months) {
        return total.subtract(monthlyPayment(total, months).multiply(BigDecimal.valueOf(months - 1L)));
    }

    /** Amount of each period, 1 to months; they add up to the total exactly. */
    public static List<BigDecimal> periodAmounts(BigDecimal total, int months) {
        List<BigDecimal> amounts = new ArrayList<>(months);
        BigDecimal monthly = monthlyPayment(total, months);
        for (int number = 1; number < months; number++) {
            amounts.add(monthly);
        }
        amounts.add(lastPayment(total, months));
        return amounts;
    }

    /** Delivery date + N months; a day missing in the target month becomes its last day (31/01 + 1 → 28 or 29/02). */
    public static LocalDate dueDate(LocalDate deliveredOn, int paymentNumber) {
        return deliveredOn.plusMonths(paymentNumber);
    }

    /** Only the last 4 digits stay visible, e.g. ******6789. */
    public static String maskCitizenId(String citizenId) {
        if (citizenId == null || citizenId.length() <= 4) {
            return citizenId;
        }
        return "*".repeat(citizenId.length() - 4) + citizenId.substring(citizenId.length() - 4);
    }
}
