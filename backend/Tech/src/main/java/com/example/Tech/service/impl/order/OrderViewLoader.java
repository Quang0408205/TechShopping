package com.example.Tech.service.impl.order;

import com.example.Tech.dto.response.order.OrderItemResponse;
import com.example.Tech.dto.response.order.OrderResponse;
import com.example.Tech.entity.order.Order;
import com.example.Tech.entity.order.OrderItem;
import com.example.Tech.entity.product.ProductImage;
import com.example.Tech.mapper.order.OrderMapper;
import com.example.Tech.repository.order.OrderItemRepository;
import com.example.Tech.repository.product.ProductImageRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Turns orders into responses with a fixed number of queries whatever the page size: one for the items of
 * all the orders (with variant and product), one for the images of their products. Shared by the customer
 * and the admin order services.
 */
@Component
@RequiredArgsConstructor
public class OrderViewLoader {

    private final OrderItemRepository orderItemRepository;
    private final ProductImageRepository imageRepository;
    private final OrderMapper orderMapper;

    public OrderResponse toResponse(Order order) {
        return toResponses(List.of(order)).getFirst();
    }

    /** Same order as the input list. */
    public List<OrderResponse> toResponses(List<Order> orders) {
        if (orders.isEmpty()) {
            return List.of();
        }
        List<Long> orderIds = orders.stream().map(Order::getId).toList();
        List<OrderItem> items = orderItemRepository.findAllWithProductByOrderIdIn(orderIds);
        Map<Long, String> imageByProduct = loadImages(items);

        Map<Long, List<OrderItemResponse>> itemsByOrder = new HashMap<>();
        for (OrderItem item : items) {
            itemsByOrder.computeIfAbsent(item.getOrder().getId(), id -> new ArrayList<>())
                    .add(orderMapper.toItemResponse(item, imageByProduct.get(item.getVariant().getProduct().getId())));
        }
        return orders.stream()
                .map(order -> orderMapper.toResponse(order, itemsByOrder.getOrDefault(order.getId(), List.of())))
                .toList();
    }

    /** A page of orders mapped with {@link #toResponses} (keeps the page numbers). */
    public Page<OrderResponse> toResponsePage(Page<Order> page) {
        Map<Long, OrderResponse> byId = toResponses(page.getContent()).stream()
                .collect(Collectors.toMap(OrderResponse::id, Function.identity()));
        return page.map(order -> byId.get(order.getId()));
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
