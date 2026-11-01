package com.example.Tech.service.impl.order;

import com.example.Tech.entity.order.Order;
import com.example.Tech.entity.order.OrderStatus;
import com.example.Tech.entity.order.OrderStatusHistory;
import com.example.Tech.entity.user.User;
import com.example.Tech.mapper.order.OrderMapper;
import com.example.Tech.repository.order.OrderStatusHistoryRepository;
import com.example.Tech.service.mail.OrderMailPublisher;
import com.example.Tech.service.notification.StaffNotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Locale;

/**
 * Writes order_status_history in the caller's transaction, at every status change (also the creation), asks for
 * the matching order mail (sent after the transaction commits) and tells the order's store about a new order or
 * an order the customer cancelled.
 */
@Component
@RequiredArgsConstructor
public class OrderStatusRecorder {

    private final OrderStatusHistoryRepository historyRepository;
    private final OrderMailPublisher mailPublisher;
    private final StaffNotificationService notificationService;

    public void record(Order order, OrderStatus from, OrderStatus to, User changedBy) {
        historyRepository.save(new OrderStatusHistory(order, from, to, changedBy));
        if (from != to) {
            mailPublisher.statusChanged(order, to);
            notifyStore(order, from, to, changedBy);
        }
    }

    private void notifyStore(Order order, OrderStatus from, OrderStatus to, User changedBy) {
        String code = OrderMapper.code(order.getId());
        String link = "orders.html?keyword=" + code;
        if (from == null && to == OrderStatus.PENDING) {
            notificationService.publish(order.getStore(), StaffNotificationService.Kind.NEW_ORDER,
                    "Đơn hàng mới " + code, "Tổng tiền %s đ".formatted(String.format(Locale.of("vi", "VN"), "%,d", order.getTotalAmount().longValue())), link);
        } else if (to == OrderStatus.CANCELLED && changedBy != null && order.getUser() != null
                && changedBy.getId().equals(order.getUser().getId())) {
            notificationService.publish(order.getStore(), StaffNotificationService.Kind.ORDER_CANCELLED,
                    "Khách đã huỷ đơn " + code, null, link);
        }
    }
}
