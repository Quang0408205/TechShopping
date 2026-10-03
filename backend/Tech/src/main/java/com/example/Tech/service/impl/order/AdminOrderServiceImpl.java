package com.example.Tech.service.impl.order;

import com.example.Tech.dto.request.order.AdminOrderSearchRequest;
import com.example.Tech.dto.request.order.OrderStatusUpdateRequest;
import com.example.Tech.dto.request.payment.InstallmentRejectRequest;
import com.example.Tech.dto.request.payment.PaymentConfirmRequest;
import com.example.Tech.dto.response.common.PageResponse;
import com.example.Tech.dto.response.order.AdminOrderResponse;
import com.example.Tech.dto.response.order.OrderResponse;
import com.example.Tech.entity.order.Order;
import com.example.Tech.entity.order.OrderStatus;
import com.example.Tech.entity.user.RoleName;
import com.example.Tech.entity.user.User;
import com.example.Tech.exception.BusinessException;
import com.example.Tech.exception.ErrorCode;
import com.example.Tech.exception.ResourceNotFoundException;
import com.example.Tech.repository.order.OrderFilterSpecifications;
import com.example.Tech.repository.order.OrderRepository;
import com.example.Tech.repository.user.CustomerProfileRepository;
import com.example.Tech.service.impl.payment.OrderPaymentLifecycle;
import com.example.Tech.service.order.AdminOrderService;
import com.example.Tech.service.user.CurrentUserLoader;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AdminOrderServiceImpl implements AdminOrderService {

    private final OrderRepository orderRepository;
    private final CustomerProfileRepository customerProfileRepository;
    private final CurrentUserLoader currentUserLoader;
    private final OrderViewLoader orderViewLoader;
    private final OrderPaymentLifecycle orderPaymentLifecycle;
    private final Clock clock;

    @Override
    public PageResponse<AdminOrderResponse> search(Long staffId, AdminOrderSearchRequest filter, Pageable pageable) {
        ensureStaff(staffId);
        if (filter.fromDate() != null && filter.toDate() != null && filter.fromDate().isAfter(filter.toDate())) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "fromDate must not be after toDate");
        }
        Page<Order> page = orderRepository.findAll(OrderFilterSpecifications.matching(filter), pageable);
        Map<Long, OrderResponse> views = orderViewLoader.toStaffResponses(page.getContent()).stream()
                .collect(Collectors.toMap(OrderResponse::id, Function.identity()));
        return PageResponse.from(page.map(order -> toAdminResponse(order, views.get(order.getId()))));
    }

    @Override
    public AdminOrderResponse getById(Long staffId, Long orderId) {
        ensureStaff(staffId);
        return toAdminResponse(findWithUser(orderId));
    }

    @Override
    @Transactional
    public AdminOrderResponse updateStatus(Long staffId, Long orderId, OrderStatusUpdateRequest request) {
        User staff = ensureStaff(staffId);
        Order order = lock(orderId);
        OrderStatus from = order.getStatus();
        OrderStatus to = request.status();
        if (from != to && !from.canMoveTo(to)) {
            throw new BusinessException(ErrorCode.INVALID_ORDER_STATUS,
                    "Order %d cannot go from %s to %s".formatted(orderId, from, to));
        }
        if (from != to && to == OrderStatus.CONFIRMED) {
            orderPaymentLifecycle.checkCanConfirm(order);
        }

        LocalDateTime now = LocalDateTime.now(clock);
        if (request.trackingNumber() != null) {
            order.setTrackingNumber(request.trackingNumber().isBlank() ? null : request.trackingNumber().trim());
        }
        if (from != to) {
            order.setStatus(to);
            if (to == OrderStatus.DELIVERED) {
                order.setDeliveredAt(now);
                orderPaymentLifecycle.onOrderDelivered(order, staff, now);
            } else if (to == OrderStatus.CANCELLED) {
                order.setCancelledAt(now);
                orderPaymentLifecycle.onOrderCancelled(order);
            }
        }
        // flushes the payment / installment changes too, before addToTotalSpent clears the persistence context
        orderRepository.saveAndFlush(order);

        if (from != to && to == OrderStatus.DELIVERED) {
            Long customerId = order.getUser().getId();
            if (customerProfileRepository.addToTotalSpent(customerId, order.getTotalAmount()) == 0) {
                log.warn("Order id={} delivered but user id={} has no customer profile: total_spent not updated",
                        orderId, customerId);
            }
        }
        log.info("Staff id={} changed order id={} from {} to {}", staffId, orderId, from, to);
        // addToTotalSpent clears the persistence context: read the order again for the response
        return toAdminResponse(findWithUser(orderId));
    }

    @Override
    @Transactional
    public AdminOrderResponse confirmPayment(Long staffId, Long orderId, PaymentConfirmRequest request) {
        User staff = ensureStaff(staffId);
        Order order = lock(orderId);
        orderPaymentLifecycle.confirmTransfer(order, staff, request != null ? request.transactionId() : null,
                LocalDateTime.now(clock));
        log.info("Staff id={} confirmed the bank transfer of order id={}", staffId, orderId);
        return toAdminResponse(findWithUser(orderId));
    }

    @Override
    @Transactional
    public AdminOrderResponse refundPayment(Long staffId, Long orderId) {
        User staff = ensureStaff(staffId);
        Order order = lock(orderId);
        orderPaymentLifecycle.confirmRefund(order, staff, LocalDateTime.now(clock));
        log.info("Staff id={} refunded order id={}", staffId, orderId);
        return toAdminResponse(findWithUser(orderId));
    }

    @Override
    @Transactional
    public AdminOrderResponse approveInstallment(Long staffId, Long orderId) {
        User staff = ensureStaff(staffId);
        Order order = lock(orderId);
        orderPaymentLifecycle.approveInstallment(order, staff, LocalDateTime.now(clock));
        log.info("Staff id={} approved the installment plan of order id={}", staffId, orderId);
        return toAdminResponse(findWithUser(orderId));
    }

    @Override
    @Transactional
    public AdminOrderResponse rejectInstallment(Long staffId, Long orderId, InstallmentRejectRequest request) {
        User staff = ensureStaff(staffId);
        Order order = lock(orderId);
        LocalDateTime now = LocalDateTime.now(clock);
        orderPaymentLifecycle.rejectInstallment(order, staff, request.reason(), now);
        order.setStatus(OrderStatus.CANCELLED);
        order.setCancelledAt(now);
        orderRepository.saveAndFlush(order);
        log.info("Staff id={} rejected the installment plan of order id={}; order cancelled", staffId, orderId);
        return toAdminResponse(findWithUser(orderId));
    }

    private User ensureStaff(Long staffId) {
        return currentUserLoader.loadWithAnyRole(staffId, RoleName.STAFF, RoleName.ADMIN);
    }

    /** SELECT … FOR UPDATE: two staff members (or staff and the customer cancelling) cannot both change it. */
    private Order lock(Long orderId) {
        return orderRepository.findByIdForUpdate(orderId)
                .orElseThrow(() -> new ResourceNotFoundException(ErrorCode.ORDER_NOT_FOUND, orderId));
    }

    private Order findWithUser(Long orderId) {
        return orderRepository.findWithUserById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException(ErrorCode.ORDER_NOT_FOUND, orderId));
    }

    private AdminOrderResponse toAdminResponse(Order order) {
        return toAdminResponse(order, orderViewLoader.toStaffResponses(List.of(order)).getFirst());
    }

    private static AdminOrderResponse toAdminResponse(Order order, OrderResponse view) {
        User user = order.getUser();
        return new AdminOrderResponse(view, new AdminOrderResponse.Customer(
                user.getId(), user.getUsername(), user.getFullname(), user.getEmail(), user.getPhone(),
                user.getDeletedAt() != null));
    }
}
