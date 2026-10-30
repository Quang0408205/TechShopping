package com.example.Tech.service.impl.payment;

import com.example.Tech.entity.order.Order;
import com.example.Tech.entity.payment.InstallmentOrder;
import com.example.Tech.entity.payment.InstallmentStatus;
import com.example.Tech.exception.BusinessException;
import com.example.Tech.exception.ErrorCode;
import com.example.Tech.repository.order.OrderRepository;
import com.example.Tech.repository.payment.InstallmentOrderRepository;
import com.example.Tech.service.payment.InstallmentDecisionService;
import com.example.Tech.service.payment.installment.InstallmentProvider;
import com.example.Tech.service.payment.installment.InstallmentProviderRegistry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDateTime;

@Slf4j
@Service
@RequiredArgsConstructor
public class InstallmentDecisionServiceImpl implements InstallmentDecisionService {

    private final InstallmentOrderRepository installmentOrderRepository;
    private final OrderRepository orderRepository;
    private final OrderPaymentLifecycle orderPaymentLifecycle;
    private final InstallmentProviderRegistry providers;
    private final Clock clock;

    @Override
    @Transactional
    public boolean applyByPartnerRef(String partnerRef, boolean approved, String reason, String eventId) {
        Long orderId = installmentOrderRepository.findOrderIdByPartnerRef(partnerRef)
                .orElseThrow(() -> new BusinessException(ErrorCode.INSTALLMENT_NOT_FOUND,
                        "Không tìm thấy hồ sơ trả góp có mã đối tác %s".formatted(partnerRef)));
        Order order = orderRepository.findByIdForUpdate(orderId).orElseThrow();
        boolean applied = orderPaymentLifecycle.applyPartnerDecision(order, approved, reason, null, eventId,
                LocalDateTime.now(clock));
        log.info("Partner answered application {} of order id={}: {}{}", partnerRef, orderId,
                approved ? "approved" : "rejected", applied ? "" : " (repeated event, ignored)");
        return applied;
    }

    @Override
    @Transactional
    public void decideAsPartner(Long orderId) {
        Order order = orderRepository.findByIdForUpdate(orderId).orElse(null);
        if (order == null) {
            return;
        }
        InstallmentOrder application = installmentOrderRepository.findByOrderId(orderId).orElse(null);
        if (application == null || application.getPartner() == null
                || application.getStatus() != InstallmentStatus.PENDING_APPROVAL) {
            return;
        }
        InstallmentProvider.Decision decision = providers.forPartner(application.getPartner().getCode()).review(application);
        orderPaymentLifecycle.applyPartnerDecision(order, decision.approved(), decision.reason(), null,
                "auto-%s-%d".formatted(application.getPartnerRef(), application.getAttemptCount()),
                LocalDateTime.now(clock));
        log.info("Simulated partner {} {} the application of order id={}", application.getPartner().getCode(),
                decision.approved() ? "approved" : "rejected", orderId);
    }
}
