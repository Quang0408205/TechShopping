package com.example.Tech.service.impl.order;

import com.example.Tech.entity.order.Order;
import com.example.Tech.entity.order.OrderStatus;
import com.example.Tech.entity.order.OrderStatusHistory;
import com.example.Tech.entity.user.User;
import com.example.Tech.repository.order.OrderStatusHistoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/** Writes order_status_history in the caller's transaction, at every status change (also the creation). */
@Component
@RequiredArgsConstructor
public class OrderStatusRecorder {

    private final OrderStatusHistoryRepository historyRepository;

    public void record(Order order, OrderStatus from, OrderStatus to, User changedBy) {
        historyRepository.save(new OrderStatusHistory(order, from, to, changedBy));
    }
}
