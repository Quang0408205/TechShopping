package com.example.Tech.dto.response.payment;

import java.math.BigDecimal;
import java.util.List;

/** Installment rules for the checkout form, so the frontend does not copy them. The partner fees are not included. */
public record InstallmentOptionsResponse(
        List<Integer> termsInMonths,
        BigDecimal minOrderTotal,
        BigDecimal interestRate,
        int citizenIdLength,
        List<Bank> banks,
        List<Partner> partners
) {

    public record Bank(String code, String name) {
    }

    /** One partner the customer can choose for the order (limited to those that accept the order total). */
    public record Partner(
            String code,
            String name,
            String type,
            boolean requiresCardBank,
            List<Integer> termsInMonths,
            BigDecimal minOrderTotal,
            BigDecimal maxOrderTotal
    ) {
    }
}
