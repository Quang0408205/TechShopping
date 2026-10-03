package com.example.Tech.dto.response.order;

import com.example.Tech.dto.response.payment.InstallmentResponse;
import com.example.Tech.dto.response.payment.PaymentResponse;
import com.example.Tech.entity.order.OrderStatus;
import com.example.Tech.entity.order.PaymentMethod;
import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public record OrderResponse(
        Long id,
        @Schema(description = "Code shown to people: DH + the id on 8 digits", example = "DH00000042")
        String code,
        OrderStatus status,
        PaymentMethod paymentMethod,
        String recipientName,
        String recipientPhone,
        String shippingAddress,
        String note,
        List<OrderItemResponse> items,
        @Schema(description = "Sum of the item quantities")
        int totalQuantity,
        @Schema(description = "Sum of the item subtotals")
        BigDecimal subtotal,
        BigDecimal shippingFee,
        BigDecimal taxAmount,
        @Schema(description = "subtotal + shippingFee + taxAmount")
        BigDecimal total,
        String trackingNumber,
        LocalDateTime orderDate,
        LocalDateTime deliveredAt,
        LocalDateTime cancelledAt,
        LocalDateTime updatedAt,
        @Schema(description = "true while the customer may still cancel (PENDING)")
        boolean cancellable,
        @Schema(description = "Main payment of a COD / BANK_TRANSFER order; null for INSTALLMENT")
        PaymentResponse payment,
        @Schema(description = "Installment plan of an INSTALLMENT order; null otherwise")
        InstallmentResponse installment
) {
}
