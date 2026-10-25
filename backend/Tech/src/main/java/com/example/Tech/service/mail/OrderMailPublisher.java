package com.example.Tech.service.mail;

import com.example.Tech.entity.order.Order;
import com.example.Tech.entity.order.OrderStatus;
import com.example.Tech.entity.order.DeliveryType;
import com.example.Tech.entity.order.PaymentMethod;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

/**
 * The one place that asks for an order mail. Status changes arrive through {@link #statusChanged} (called by
 * OrderStatusRecorder, which every status change already goes through); a received payment through
 * {@link #paymentReceived}.
 */
@Component
@RequiredArgsConstructor
public class OrderMailPublisher {

    private final ApplicationEventPublisher events;

    /** QR / card orders are mailed once the money is in (see {@link #paymentReceived}), not when they are placed. */
    public static boolean paidOnline(Order order) {
        return order.getPaymentMethod() == PaymentMethod.BANK_TRANSFER || order.getPaymentMethod() == PaymentMethod.CARD;
    }

    public void statusChanged(Order order, OrderStatus to) {
        if (to == OrderStatus.PENDING && paidOnline(order)) {
            return;
        }
        MailKind kind = switch (to) {
            case PENDING -> MailKind.ORDER_PLACED;
            case CONFIRMED -> MailKind.ORDER_CONFIRMED;
            case SHIPPING -> order.getDeliveryType() == DeliveryType.PICKUP ? MailKind.READY_FOR_PICKUP : MailKind.ORDER_SHIPPING;
            case DELIVERED -> MailKind.ORDER_DELIVERED;
            case CANCELLED -> MailKind.ORDER_CANCELLED;
        };
        events.publishEvent(new OrderMailEvent(order.getId(), kind));
    }

    public void paymentReceived(Order order) {
        events.publishEvent(new OrderMailEvent(order.getId(), MailKind.PAYMENT_RECEIVED));
    }
}
