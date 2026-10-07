package com.example.Tech.dto.response.payment;

import com.example.Tech.dto.response.order.AdminOrderResponse;
import com.example.Tech.entity.order.OrderStatus;
import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/** One row of the staff installment list: the plan (complete citizen id), its order and customer. */
public record AdminInstallmentResponse(
        InstallmentResponse installment,
        OrderSummary order,
        AdminOrderResponse.Customer customer,
        @Schema(description = "Earliest unpaid period, the only one that can be recorded; null when none")
        InstallmentPeriodResponse nextPeriod
) {

    public record OrderSummary(
            Long id,
            String code,
            OrderStatus status,
            LocalDateTime orderDate,
            LocalDateTime deliveredAt,
            BigDecimal total,
            String recipientName,
            String recipientPhone
    ) {
    }
}
