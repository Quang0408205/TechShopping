package com.example.Tech.mapper.order;

import com.example.Tech.dto.response.order.OrderItemResponse;
import com.example.Tech.dto.response.order.OrderResponse;
import com.example.Tech.dto.response.payment.InstallmentResponse;
import com.example.Tech.dto.response.payment.PaymentResponse;
import com.example.Tech.entity.order.Order;
import com.example.Tech.entity.order.OrderItem;
import com.example.Tech.entity.product.Product;
import com.example.Tech.entity.product.ProductVariant;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

/**
 * Builds order responses. Prices come from the order (fixed at checkout); names and images come from the
 * current catalogue (variants of an order are never hard-deleted).
 */
@Component
public class OrderMapper {

    /** "DH" + the id on 8 digits, e.g. DH00000042 (no extra column, decision 2026-10-02). */
    public static String code(Long orderId) {
        return "DH%08d".formatted(orderId);
    }

    public OrderResponse toResponse(Order order, List<OrderItemResponse> items, PaymentResponse payment,
                                    InstallmentResponse installment) {
        int totalQuantity = items.stream().mapToInt(OrderItemResponse::quantity).sum();
        BigDecimal subtotal = items.stream()
                .map(OrderItemResponse::subtotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        return new OrderResponse(
                order.getId(),
                code(order.getId()),
                order.getStatus(),
                order.getPaymentMethod(),
                order.getRecipientName(),
                order.getRecipientPhone(),
                order.getShippingAddress(),
                order.getNotes(),
                items,
                totalQuantity,
                subtotal,
                order.getShippingCost(),
                order.getTaxAmount(),
                order.getTotalAmount(),
                order.getTrackingNumber(),
                order.getOrderDate(),
                order.getDeliveredAt(),
                order.getCancelledAt(),
                order.getUpdatedAt(),
                order.getStatus() != null && order.getStatus().canBeCancelledByCustomer(),
                payment,
                installment);
    }

    public OrderItemResponse toItemResponse(OrderItem item, String imageUrl) {
        ProductVariant variant = item.getVariant();
        Product product = variant.getProduct();
        BigDecimal discount = item.getDiscountAmount() != null ? item.getDiscountAmount() : BigDecimal.ZERO;
        BigDecimal originalPrice = discount.signum() > 0
                ? item.getUnitPrice().add(discount.divide(BigDecimal.valueOf(item.getQuantity()), 2, RoundingMode.HALF_UP))
                : null;
        return new OrderItemResponse(
                item.getId(),
                variant.getId(),
                product.getId(),
                product.getName(),
                product.getSlug(),
                variant.getVariantName(),
                imageUrl,
                item.getUnitPrice(),
                originalPrice,
                item.getQuantity(),
                discount,
                item.getSubtotal());
    }
}
