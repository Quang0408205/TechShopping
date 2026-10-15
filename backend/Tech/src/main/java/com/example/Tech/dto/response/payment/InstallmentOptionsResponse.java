package com.example.Tech.dto.response.payment;

import java.math.BigDecimal;
import java.util.List;

/** Installment rules for the checkout form, so the frontend does not copy them. */
public record InstallmentOptionsResponse(
        List<Integer> termsInMonths,
        BigDecimal minOrderTotal,
        BigDecimal interestRate,
        int citizenIdLength,
        List<Bank> banks
) {

    public record Bank(String code, String name) {
    }
}
