package com.example.Tech.service.impl.payment;

import com.example.Tech.dto.request.payment.AdminInstallmentSearchRequest;
import com.example.Tech.dto.request.payment.PaymentConfirmRequest;
import com.example.Tech.dto.response.common.PageResponse;
import com.example.Tech.dto.response.order.AdminOrderResponse;
import com.example.Tech.dto.response.payment.AdminInstallmentResponse;
import com.example.Tech.dto.response.payment.InstallmentPeriodResponse;
import com.example.Tech.dto.response.payment.InstallmentResponse;
import com.example.Tech.entity.order.Order;
import com.example.Tech.entity.payment.InstallmentOrder;
import com.example.Tech.entity.payment.InstallmentPayment;
import com.example.Tech.entity.payment.InstallmentPaymentStatus;
import com.example.Tech.entity.user.User;
import com.example.Tech.exception.BusinessException;
import com.example.Tech.exception.ErrorCode;
import com.example.Tech.mapper.order.OrderMapper;
import com.example.Tech.mapper.payment.PaymentMapper;
import com.example.Tech.repository.order.OrderRepository;
import com.example.Tech.repository.payment.InstallmentFilterSpecifications;
import com.example.Tech.repository.payment.InstallmentOrderRepository;
import com.example.Tech.repository.payment.InstallmentPaymentRepository;
import com.example.Tech.service.payment.AdminInstallmentService;
import com.example.Tech.service.store.StoreAccessGuard;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AdminInstallmentServiceImpl implements AdminInstallmentService {

    private final InstallmentOrderRepository installmentOrderRepository;
    private final InstallmentPaymentRepository installmentPaymentRepository;
    private final OrderRepository orderRepository;
    private final OrderPaymentLifecycle orderPaymentLifecycle;
    private final PaymentMapper paymentMapper;
    private final StoreAccessGuard storeAccessGuard;
    private final Clock clock;

    @Override
    public PageResponse<AdminInstallmentResponse> search(Long staffId, AdminInstallmentSearchRequest filter,
                                                         Pageable pageable) {
        StoreAccessGuard.OrderScope scope = storeAccessGuard.orderScope(staffId);
        LocalDate today = LocalDate.now(clock);
        Page<InstallmentOrder> page = installmentOrderRepository.findAll(
                InstallmentFilterSpecifications.matching(filter, today, scope.storeId()), pageable);
        Map<Long, AdminInstallmentResponse> byId = toResponses(page.getContent(), today).stream()
                .collect(Collectors.toMap(response -> response.installment().id(), Function.identity()));
        return PageResponse.from(page.map(plan -> byId.get(plan.getId())));
    }

    @Override
    public AdminInstallmentResponse getById(Long staffId, Long installmentId) {
        StoreAccessGuard.OrderScope scope = storeAccessGuard.orderScope(staffId);
        InstallmentOrder plan = findWithOrder(installmentId);
        scope.check(plan.getOrder());
        return toResponse(plan);
    }

    @Override
    @Transactional
    public AdminInstallmentResponse payPeriod(Long staffId, Long installmentId, int number, PaymentConfirmRequest request) {
        StoreAccessGuard.OrderScope scope = storeAccessGuard.orderScope(staffId);
        Long orderId = installmentOrderRepository.findOrderIdById(installmentId).orElseThrow(() -> notFound(installmentId));
        // same lock as every order change: two staff members cannot record the same period twice. The plan is read
        // only after the lock (the scalar query above does not load it), so its status is current.
        orderRepository.findByIdForUpdate(orderId).ifPresent(scope::check);
        InstallmentOrder plan = findWithOrder(installmentId);
        orderPaymentLifecycle.payPeriod(plan, number, scope.user(), request != null ? request.transactionId() : null,
                LocalDateTime.now(clock));
        log.info("Staff id={} recorded period {} of installment plan id={}", staffId, number, installmentId);
        return toResponse(plan);
    }

    private InstallmentOrder findWithOrder(Long installmentId) {
        return installmentOrderRepository.findWithOrderById(installmentId).orElseThrow(() -> notFound(installmentId));
    }

    private static BusinessException notFound(Long installmentId) {
        return new BusinessException(ErrorCode.INSTALLMENT_NOT_FOUND,
                "Không tìm thấy hợp đồng trả góp #%d".formatted(installmentId));
    }

    private AdminInstallmentResponse toResponse(InstallmentOrder plan) {
        return toResponses(List.of(plan), LocalDate.now(clock)).getFirst();
    }

    /** Schedules of all the plans in one query; same order as the input. */
    private List<AdminInstallmentResponse> toResponses(List<InstallmentOrder> plans, LocalDate today) {
        if (plans.isEmpty()) {
            return List.of();
        }
        Map<Long, List<InstallmentPayment>> periodsByPlan = installmentPaymentRepository
                .findAllByInstallmentIdIn(plans.stream().map(InstallmentOrder::getId).toList())
                .stream()
                .collect(Collectors.groupingBy(period -> period.getInstallment().getId()));
        return plans.stream().map(plan -> {
            Order order = plan.getOrder();
            InstallmentResponse installment = paymentMapper.toResponse(plan, order,
                    periodsByPlan.getOrDefault(plan.getId(), List.of()), today, true);
            InstallmentPeriodResponse nextPeriod = installment.periods().stream()
                    .filter(period -> period.status() == InstallmentPaymentStatus.PENDING)
                    .findFirst()
                    .orElse(null);
            User customer = order.getUser();
            return new AdminInstallmentResponse(
                    installment,
                    new AdminInstallmentResponse.OrderSummary(order.getId(), OrderMapper.code(order.getId()),
                            order.getStatus(), order.getOrderDate(), order.getDeliveredAt(), order.getTotalAmount(),
                            order.getRecipientName(), order.getRecipientPhone()),
                    new AdminOrderResponse.Customer(customer.getId(), customer.getUsername(), customer.getFullname(),
                            customer.getEmail(), customer.getPhone(), customer.getDeletedAt() != null),
                    nextPeriod);
        }).toList();
    }
}
