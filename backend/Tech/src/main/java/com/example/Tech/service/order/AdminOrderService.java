package com.example.Tech.service.order;

import com.example.Tech.dto.request.order.AdminOrderSearchRequest;
import com.example.Tech.dto.request.order.OrderStatusUpdateRequest;
import com.example.Tech.dto.request.order.OrderStoreRequest;
import com.example.Tech.dto.request.payment.InstallmentRejectRequest;
import com.example.Tech.dto.request.payment.PaymentConfirmRequest;
import com.example.Tech.dto.response.common.PageResponse;
import com.example.Tech.dto.response.order.AdminOrderResponse;
import org.springframework.data.domain.Pageable;

/**
 * Order management for staff (STAFF or ADMIN). Every call re-checks that the caller still exists, is enabled
 * and has STAFF or ADMIN in the database (403 ACCESS_DENIED otherwise). Store scoping (Phase 7): an ADMIN sees and
 * acts on every order; a STAFF member only on the orders of their current store (403 NO_ACTIVE_STORE_ASSIGNMENT
 * without one, 403 ACCESS_DENIED for another store's order or an order without a store; the list is filtered).
 * Every change locks the order row first.
 */
public interface AdminOrderService {

    PageResponse<AdminOrderResponse> search(Long staffId, AdminOrderSearchRequest filter, Pageable pageable);

    /** 404 ORDER_NOT_FOUND. */
    AdminOrderResponse getById(Long staffId, Long orderId);

    /**
     * Moves the order along the flow (OrderStatus.canMoveTo), else 409 INVALID_ORDER_STATUS. The same status
     * only updates the tracking number. CONFIRMED needs a paid bank transfer (409 PAYMENT_REQUIRED) or an approved
     * installment plan (409 INSTALLMENT_NOT_APPROVED), a store (409 ORDER_STORE_MISSING) with enough stock
     * (409 INSUFFICIENT_STOCK), and takes the quantities from that store. DELIVERED sets deliveredAt, adds the total to
     * the customer's total_spent, marks a COD payment PAID and starts an installment schedule; CANCELLED sets
     * cancelledAt, cancels / queues for refund the payment, cancels a plan not started and, from CONFIRMED, puts the
     * stock back.
     */
    AdminOrderResponse updateStatus(Long staffId, Long orderId, OrderStatusUpdateRequest request);

    /** Bank transfer received: PENDING → PAID. 404 PAYMENT_NOT_FOUND, 409 INVALID_PAYMENT_STATUS (also for COD). */
    AdminOrderResponse confirmPayment(Long staffId, Long orderId, PaymentConfirmRequest request);

    /** Money of a cancelled paid order sent back: REFUND_PENDING → REFUNDED. 409 INVALID_PAYMENT_STATUS. */
    AdminOrderResponse refundPayment(Long staffId, Long orderId);

    /** PENDING_APPROVAL → APPROVED. 404 INSTALLMENT_NOT_FOUND, 409 INVALID_INSTALLMENT_STATUS. */
    AdminOrderResponse approveInstallment(Long staffId, Long orderId);

    /** PENDING_APPROVAL → REJECTED with the reason; the order is cancelled. */
    AdminOrderResponse rejectInstallment(Long staffId, Long orderId, InstallmentRejectRequest request);

    /**
     * ADMIN only: moves a PENDING order to another open store (pickup orders get the new store's address).
     * 409 INVALID_ORDER_STATUS once confirmed, 404 STORE_NOT_FOUND, 400 details.storeId for a closed store.
     */
    AdminOrderResponse reassignStore(Long adminId, Long orderId, OrderStoreRequest request);
}
