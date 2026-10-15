package com.example.Tech.repository.order;

import com.example.Tech.entity.order.OrderStatus;
import com.example.Tech.entity.order.OrderStatusHistory;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface OrderStatusHistoryRepository extends JpaRepository<OrderStatusHistory, Long> {

    List<OrderStatusHistory> findAllByOrder_IdOrderByChangedAtAscIdAsc(Long orderId);

    Optional<OrderStatusHistory> findFirstByOrder_IdAndNewStatusOrderByChangedAtDescIdDesc(Long orderId,
                                                                                         OrderStatus newStatus);
}
