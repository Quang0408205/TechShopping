package com.example.Tech.dto.response.order;

import com.example.Tech.entity.order.DeliveryType;
import com.example.Tech.entity.order.OrderStatus;
import com.example.Tech.entity.order.PaymentMethod;
import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * What the public tracking page (opened from the email link) shows. No phone number, no street address,
 * no account data: only the order code, the products, the money and the progress.
 */
public record OrderTrackingResponse(
        @Schema(example = "DH00000042")
        String code,
        OrderStatus status,
        PaymentMethod paymentMethod,
        @Schema(description = "PENDING, PAID, CANCELLED, REFUND_PENDING or REFUNDED; null when the order has no payment row")
        String paymentStatus,
        DeliveryType deliveryType,
        @Schema(description = "Pickup store name, or the ward and province of a home delivery")
        String receiving,
        String trackingNumber,
        List<Item> items,
        BigDecimal total,
        LocalDateTime orderDate,
        @Schema(description = "The statuses the order went through, oldest first")
        List<Step> steps
) {

    public record Item(String name, int quantity, BigDecimal subtotal) {
    }

    public record Step(OrderStatus status, LocalDateTime at) {
    }
}
