package com.example.Tech.entity.order;

import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class OrderStatusTest {

    @Test
    void staffTransitions_followTheFlow() {
        assertThat(nextOf(OrderStatus.PENDING)).containsExactly(OrderStatus.CONFIRMED, OrderStatus.CANCELLED);
        assertThat(nextOf(OrderStatus.CONFIRMED)).containsExactly(OrderStatus.SHIPPING, OrderStatus.CANCELLED);
        assertThat(nextOf(OrderStatus.SHIPPING)).containsExactly(OrderStatus.DELIVERED);
        assertThat(nextOf(OrderStatus.DELIVERED)).isEmpty();
        assertThat(nextOf(OrderStatus.CANCELLED)).isEmpty();
    }

    @Test
    void customer_canOnlyCancelAPendingOrder() {
        assertThat(Arrays.stream(OrderStatus.values()).filter(OrderStatus::canBeCancelledByCustomer))
                .containsExactly(OrderStatus.PENDING);
    }

    private static List<OrderStatus> nextOf(OrderStatus status) {
        return Arrays.stream(OrderStatus.values()).filter(status::canMoveTo).toList();
    }
}
