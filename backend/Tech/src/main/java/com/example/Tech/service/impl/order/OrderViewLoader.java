package com.example.Tech.service.impl.order;

import com.example.Tech.dto.response.order.OrderItemResponse;
import com.example.Tech.dto.response.order.OrderResponse;
import com.example.Tech.dto.response.payment.InstallmentResponse;
import com.example.Tech.dto.response.payment.PaymentResponse;
import com.example.Tech.entity.order.Order;
import com.example.Tech.entity.order.OrderItem;
import com.example.Tech.entity.payment.InstallmentOrder;
import com.example.Tech.entity.payment.InstallmentPayment;
import com.example.Tech.entity.payment.Payment;
import com.example.Tech.entity.product.ProductImage;
import com.example.Tech.mapper.order.OrderMapper;
import com.example.Tech.mapper.payment.PaymentMapper;
import com.example.Tech.repository.order.OrderItemRepository;
import com.example.Tech.repository.payment.InstallmentOrderRepository;
import com.example.Tech.repository.payment.InstallmentPaymentRepository;
import com.example.Tech.repository.payment.PaymentRepository;
import com.example.Tech.repository.product.ProductImageRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Turns orders into responses with a fixed number of queries whatever the page size: items (with variant and
 * product), product images, main payments, installment plans and, when there are plans, their schedules.
 * Shared by the customer and the admin order services; only staff sees the full citizen id.
 */
@Component
@RequiredArgsConstructor
public class OrderViewLoader {

    private final OrderItemRepository orderItemRepository;
    private final ProductImageRepository imageRepository;
    private final PaymentRepository paymentRepository;
    private final InstallmentOrderRepository installmentOrderRepository;
    private final InstallmentPaymentRepository installmentPaymentRepository;
    private final OrderMapper orderMapper;
    private final PaymentMapper paymentMapper;
    private final Clock clock;

    public OrderResponse toResponse(Order order) {
        return toResponses(List.of(order)).getFirst();
    }

    /** Customer view, same order as the input list. */
    public List<OrderResponse> toResponses(List<Order> orders) {
        return toResponses(orders, false);
    }

    /** Staff view: like {@link #toResponses(List)} with the complete citizen id. */
    public List<OrderResponse> toStaffResponses(List<Order> orders) {
        return toResponses(orders, true);
    }

    /** A page of orders mapped with {@link #toResponses(List)} (keeps the page numbers). */
    public Page<OrderResponse> toResponsePage(Page<Order> page) {
        Map<Long, OrderResponse> byId = toResponses(page.getContent()).stream()
                .collect(Collectors.toMap(OrderResponse::id, Function.identity()));
        return page.map(order -> byId.get(order.getId()));
    }

    private List<OrderResponse> toResponses(List<Order> orders, boolean staffView) {
        if (orders.isEmpty()) {
            return List.of();
        }
        List<Long> orderIds = orders.stream().map(Order::getId).toList();
        Map<Long, Order> orderById = orders.stream().collect(Collectors.toMap(Order::getId, Function.identity()));

        List<OrderItem> items = orderItemRepository.findAllWithProductByOrderIdIn(orderIds);
        Map<Long, String> imageByProduct = loadImages(items);
        Map<Long, List<OrderItemResponse>> itemsByOrder = new HashMap<>();
        for (OrderItem item : items) {
            itemsByOrder.computeIfAbsent(item.getOrder().getId(), id -> new ArrayList<>())
                    .add(orderMapper.toItemResponse(item, imageByProduct.get(item.getVariant().getProduct().getId())));
        }

        Map<Long, PaymentResponse> paymentByOrder = new HashMap<>();
        for (Payment payment : paymentRepository.findAllByOrderIdInAndInstallmentPaymentIsNull(orderIds)) {
            Long orderId = payment.getOrder().getId();
            paymentByOrder.put(orderId, paymentMapper.toResponse(payment, orderById.get(orderId)));
        }
        Map<Long, InstallmentResponse> installmentByOrder = loadInstallments(orderIds, orderById, staffView);

        return orders.stream()
                .map(order -> orderMapper.toResponse(order, itemsByOrder.getOrDefault(order.getId(), List.of()),
                        paymentByOrder.get(order.getId()), installmentByOrder.get(order.getId())))
                .toList();
    }

    private Map<Long, InstallmentResponse> loadInstallments(List<Long> orderIds, Map<Long, Order> orderById,
                                                            boolean staffView) {
        List<InstallmentOrder> plans = installmentOrderRepository.findAllByOrderIdIn(orderIds);
        if (plans.isEmpty()) {
            return Map.of();
        }
        Map<Long, List<InstallmentPayment>> periodsByPlan = installmentPaymentRepository
                .findAllByInstallmentIdIn(plans.stream().map(InstallmentOrder::getId).toList())
                .stream()
                .collect(Collectors.groupingBy(period -> period.getInstallment().getId()));
        LocalDate today = LocalDate.now(clock);
        Map<Long, InstallmentResponse> byOrder = new HashMap<>();
        for (InstallmentOrder plan : plans) {
            Long orderId = plan.getOrder().getId();
            byOrder.put(orderId, paymentMapper.toResponse(plan, orderById.get(orderId),
                    periodsByPlan.getOrDefault(plan.getId(), List.of()), today, staffView));
        }
        return byOrder;
    }

    /** One image per product (the first of the best-first order), in one query. */
    private Map<Long, String> loadImages(List<OrderItem> items) {
        List<Long> productIds = items.stream()
                .map(item -> item.getVariant().getProduct().getId())
                .distinct()
                .toList();
        if (productIds.isEmpty()) {
            return Map.of();
        }
        Map<Long, String> imageByProduct = new HashMap<>();
        for (ProductImage image : imageRepository.findAllByProductIdInBestFirst(productIds)) {
            imageByProduct.putIfAbsent(image.getProduct().getId(), image.getImageUrl());
        }
        return imageByProduct;
    }
}
