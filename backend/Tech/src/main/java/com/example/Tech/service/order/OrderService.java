package com.example.Tech.service.order;

import com.example.Tech.dto.request.order.OrderCreateRequest;
import com.example.Tech.dto.response.common.PageResponse;
import com.example.Tech.dto.response.order.OrderResponse;
import org.springframework.data.domain.Pageable;

/**
 * The logged-in customer's orders (the id comes from the access token). Every call re-checks the account.
 */
public interface OrderService {

    /**
     * Places an order from the user's server cart in one transaction: the cart row is locked, prices are
     * taken from the variants, the shipping fee from ShippingPolicy, then the cart is emptied.
     * Empty cart → 409 CART_EMPTY (also the answer to a double submit); an unavailable line → 409
     * PRODUCT_NOT_AVAILABLE.
     */
    OrderResponse placeOrder(Long userId, OrderCreateRequest request);

    PageResponse<OrderResponse> getMyOrders(Long userId, Pageable pageable);

    /** Another user's order → 404 ORDER_NOT_FOUND. */
    OrderResponse getMyOrder(Long userId, Long orderId);

    /** Only a PENDING order (else 409 INVALID_ORDER_STATUS). */
    OrderResponse cancelMyOrder(Long userId, Long orderId);
}
