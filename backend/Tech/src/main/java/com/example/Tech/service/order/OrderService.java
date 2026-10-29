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

    /** Plan v5 GĐ6 purchase history: my orders, optionally by statuses and a keyword (order code or product name). */
    PageResponse<OrderResponse> getMyOrders(Long userId, java.util.List<com.example.Tech.entity.order.OrderStatus> statuses,
                                            String keyword, Pageable pageable);

    /** Plan v5 GĐ6: my orders per history tab. */
    com.example.Tech.dto.response.order.OrderCountsResponse countMyOrders(Long userId);

    /** Another user's order → 404 ORDER_NOT_FOUND. */
    OrderResponse getMyOrder(Long userId, Long orderId);

    /** Only a PENDING order (else 409 INVALID_ORDER_STATUS). */
    OrderResponse cancelMyOrder(Long userId, Long orderId);

    /**
     * "Huỷ thanh toán" (plan v5 GĐ5): cancels my PENDING bank-transfer / card order whose money has not arrived
     * (else 409 INVALID_PAYMENT_STATUS), gives the voucher use back and puts the ordered items back into my cart.
     */
    OrderResponse cancelPayment(Long userId, Long orderId);

    /** My newest PENDING bank-transfer / card order still waiting for its money within the payment deadline, or null. */
    OrderResponse findAwaitingPayment(Long userId);
}
