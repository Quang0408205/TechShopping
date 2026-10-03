package com.example.Tech.dto.response.payment;

import com.example.Tech.entity.payment.InstallmentPaymentStatus;
import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.time.LocalDate;

public record InstallmentPeriodResponse(
        Long id,
        int number,
        BigDecimal amount,
        LocalDate dueDate,
        LocalDate paidDate,
        InstallmentPaymentStatus status,
        @Schema(description = "PENDING and the due date is before today")
        boolean overdue
) {
}
