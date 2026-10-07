package com.example.Tech.dto.response.payment;

import com.example.Tech.entity.payment.InstallmentBank;
import com.example.Tech.entity.payment.InstallmentStatus;
import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public record InstallmentResponse(
        Long id,
        InstallmentStatus status,
        int numMonths,
        BigDecimal monthlyPayment,
        @Schema(description = "Amount of the last period (takes the rounding remainder)")
        BigDecimal lastPayment,
        @Schema(description = "Order total (0% interest, no down payment)")
        BigDecimal totalAmount,
        BigDecimal interestRate,
        @Schema(description = "Masked (******6789) for the customer, complete for staff")
        String citizenId,
        InstallmentBank cardBank,
        String cardBankName,
        String rejectionReason,
        LocalDateTime reviewedAt,
        int paidPeriods,
        BigDecimal paidAmount,
        BigDecimal remainingAmount,
        @Schema(description = "Empty until the order is delivered")
        List<InstallmentPeriodResponse> periods
) {
}
