package com.example.Tech.service.order;

import com.example.Tech.dto.request.order.AdminOrderSearchRequest;
import com.example.Tech.dto.request.order.OrderStatusUpdateRequest;
import com.example.Tech.dto.response.common.PageResponse;
import com.example.Tech.dto.response.order.AdminOrderResponse;
import org.springframework.data.domain.Pageable;

/**
 * Order management for staff (STAFF or ADMIN). Every call re-checks that the caller still exists, is enabled
 * and has STAFF or ADMIN in the database (403 ACCESS_DENIED otherwise). There is no store scoping until
 * Phase 7 (sales_records).
 */
public interface AdminOrderService {

    PageResponse<AdminOrderResponse> search(Long staffId, AdminOrderSearchRequest filter, Pageable pageable);

    /** 404 ORDER_NOT_FOUND. */
    AdminOrderResponse getById(Long staffId, Long orderId);

    /**
     * Moves the order along the flow (OrderStatus.canMoveTo), else 409 INVALID_ORDER_STATUS. The same status
     * only updates the tracking number. DELIVERED sets deliveredAt and adds the total to the customer's
     * total_spent; CANCELLED sets cancelledAt.
     */
    AdminOrderResponse updateStatus(Long staffId, Long orderId, OrderStatusUpdateRequest request);
}
