package com.example.Tech.dto.response.payment;

import com.example.Tech.entity.payment.InstallmentPartnerType;

import java.math.BigDecimal;
import java.util.List;

/** An installment partner with its conditions and fee table (ADMIN). */
public record InstallmentPartnerResponse(
        String code,
        String name,
        InstallmentPartnerType type,
        boolean active,
        BigDecimal minOrderTotal,
        BigDecimal maxOrderTotal,
        List<Rate> rates
) {

    public record Rate(int numMonths, BigDecimal feePercent) {
    }
}
