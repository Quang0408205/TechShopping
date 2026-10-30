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
        @Schema(description = "Empty for applications handled by a partner (the partner collects the monthly amounts)")
        List<InstallmentPeriodResponse> periods,
        @Schema(description = "Partner code; null on plans from before partners existed")
        String partner,
        String partnerName,
        @Schema(description = "Reference the partner gave to the application (staff only)")
        String partnerRef,
        @Schema(description = "How many times the application was sent (1 = never sent again after a rejection)")
        int attemptCount,
        @Schema(description = "Partner fee in percent (staff only; the customer never sees it)")
        BigDecimal feePercent,
        @Schema(description = "Partner fee in đồng, fixed when the partner approved (staff only)")
        BigDecimal feeAmount,
        @Schema(description = "What the partner pays the shop = total - fee (staff only)")
        BigDecimal netAmount,
        @Schema(description = "When the partner paid the shop; null until then")
        LocalDateTime settledAt
) {
}
