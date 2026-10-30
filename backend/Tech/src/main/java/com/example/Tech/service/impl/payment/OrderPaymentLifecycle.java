package com.example.Tech.service.impl.payment;

import com.example.Tech.config.StripeProperties;
import com.example.Tech.dto.request.payment.InstallmentRequest;
import com.example.Tech.entity.order.Order;
import com.example.Tech.entity.order.OrderStatus;
import com.example.Tech.entity.order.PaymentMethod;
import com.example.Tech.entity.payment.InstallmentOrder;
import com.example.Tech.entity.payment.InstallmentPartner;
import com.example.Tech.entity.payment.InstallmentStatus;
import com.example.Tech.entity.payment.Payment;
import com.example.Tech.entity.payment.PaymentStatus;
import com.example.Tech.entity.user.User;
import com.example.Tech.exception.BusinessException;
import com.example.Tech.exception.ErrorCode;
import com.example.Tech.mapper.order.OrderMapper;
import com.example.Tech.repository.payment.InstallmentOrderRepository;
import com.example.Tech.repository.payment.PaymentRepository;
import com.example.Tech.service.loyalty.LoyaltyService;
import com.example.Tech.service.mail.OrderMailPublisher;
import com.example.Tech.service.payment.InstallmentPolicy;
import com.example.Tech.service.payment.StripeGateway;
import com.example.Tech.service.payment.installment.InstallmentAutoDecider;
import com.example.Tech.service.payment.installment.InstallmentFeePolicy;
import com.example.Tech.service.payment.installment.InstallmentPartnerPolicy;
import com.example.Tech.service.payment.installment.InstallmentProviderRegistry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Payment side of the order life cycle. Runs inside the caller's transaction, after the caller has locked the order
 * row (or created it), so the payment rows never change concurrently.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class OrderPaymentLifecycle {

    private final PaymentRepository paymentRepository;
    private final InstallmentOrderRepository installmentOrderRepository;
    private final InstallmentPartnerPolicy partnerPolicy;
    private final InstallmentFeePolicy feePolicy;
    private final InstallmentProviderRegistry providers;
    private final InstallmentAutoDecider autoDecider;
    private final StripeProperties stripe;
    private final StripeGateway stripeGateway;
    private final OrderMailPublisher mailPublisher;
    private final LoyaltyService loyaltyService;

    /** Checks that installment data comes with INSTALLMENT and only with it, and fits its partner; called before the cart is read. */
    public void validateSelection(PaymentMethod method, InstallmentRequest installment) {
        if (method == PaymentMethod.CARD && !stripe.enabled()) {
            throw new BusinessException(ErrorCode.CARD_PAYMENT_DISABLED);
        }
        if (method != PaymentMethod.INSTALLMENT) {
            if (installment != null) {
                throw BusinessException.invalidField("installment", "Chỉ gửi thông tin trả góp khi chọn thanh toán trả góp");
            }
            return;
        }
        if (installment == null) {
            throw BusinessException.invalidField("installment", "Vui lòng nhập thông tin trả góp");
        }
        partnerPolicy.resolve(installment);
    }

    /**
     * An installment order total must be inside the range of the partner chosen; a card payment at most
     * {@link StripeProperties#MAX_AMOUNT_VND}.
     */
    public void checkEligible(PaymentMethod method, BigDecimal orderTotal, InstallmentRequest installment) {
        if (method == PaymentMethod.INSTALLMENT) {
            partnerPolicy.checkAmount(partnerPolicy.resolve(installment), orderTotal);
        }
        if (method == PaymentMethod.CARD && orderTotal.compareTo(BigDecimal.valueOf(StripeProperties.MAX_AMOUNT_VND)) > 0) {
            throw new BusinessException(ErrorCode.CARD_AMOUNT_TOO_LARGE);
        }
    }

    /**
     * COD / BANK_TRANSFER / CARD: one PENDING main payment of the total. INSTALLMENT: the same payment (it becomes PAID
     * when the partner approves) plus the application, sent to the partner the customer chose.
     */
    public void onOrderPlaced(Order order, InstallmentRequest installment) {
        if (order.getPaymentMethod() == PaymentMethod.INSTALLMENT) {
            InstallmentPartner partner = partnerPolicy.resolve(installment);
            InstallmentOrder application = new InstallmentOrder();
            application.setOrder(order);
            fill(application, partner, installment, order.getTotalAmount());
            installmentOrderRepository.save(application);
            autoDecider.scheduleAfterCommit(order.getId());
        }
        Payment payment = new Payment();
        payment.setOrder(order);
        payment.setAmount(order.getTotalAmount());
        payment.setPaymentMethod(order.getPaymentMethod());
        paymentRepository.save(payment);
    }

    /** PENDING → CONFIRMED: a bank transfer / card payment must be paid, an installment application approved. */
    public void checkCanConfirm(Order order) {
        String code = OrderMapper.code(order.getId());
        if (order.getPaymentMethod() == PaymentMethod.BANK_TRANSFER || order.getPaymentMethod() == PaymentMethod.CARD) {
            boolean paid = paymentRepository.findByOrderIdAndInstallmentPaymentIsNull(order.getId())
                    .map(payment -> payment.getStatus() == PaymentStatus.PAID)
                    .orElse(false);
            if (!paid) {
                throw new BusinessException(ErrorCode.PAYMENT_REQUIRED, (order.getPaymentMethod() == PaymentMethod.CARD
                        ? "Đơn %s chưa được thanh toán bằng thẻ" : "Đơn %s chưa được xác nhận đã nhận tiền chuyển khoản")
                        .formatted(code));
            }
        } else if (order.getPaymentMethod() == PaymentMethod.INSTALLMENT) {
            boolean approved = installmentOrderRepository.findByOrderId(order.getId())
                    .map(plan -> plan.getStatus() == InstallmentStatus.APPROVED || plan.getStatus() == InstallmentStatus.SETTLED)
                    .orElse(false);
            if (!approved) {
                throw new BusinessException(ErrorCode.INSTALLMENT_NOT_APPROVED,
                        "Hồ sơ trả góp của đơn %s chưa được đối tác duyệt".formatted(code));
            }
        }
    }

    /**
     * Delivered: COD money is collected by the courier (the order counts for points then). Installment money comes
     * from the partner (see settle).
     */
    public void onOrderDelivered(Order order, User staff, LocalDateTime now) {
        if (order.getPaymentMethod() == PaymentMethod.COD) {
            paymentRepository.findByOrderIdAndInstallmentPaymentIsNull(order.getId())
                    .filter(payment -> payment.getStatus() == PaymentStatus.PENDING)
                    .ifPresent(payment -> {
                        payment.setStatus(PaymentStatus.PAID);
                        payment.setPaidAt(now);
                        payment.setConfirmedBy(staff);
                        loyaltyService.onPaid(order);
                    });
        }
    }

    /**
     * The order was just cancelled: an awaited payment is cancelled, money already received waits for a refund (and
     * the points / spending it counted are taken back), an installment application is withdrawn from its partner. An
     * installment order only owes a refund once the partner has paid the shop.
     */
    public void onOrderCancelled(Order order) {
        InstallmentOrder plan = installmentOrderRepository.findByOrderId(order.getId()).orElse(null);
        boolean partnerPaid = plan != null && plan.getStatus() == InstallmentStatus.SETTLED;
        paymentRepository.findByOrderIdAndInstallmentPaymentIsNull(order.getId()).ifPresent(payment -> {
            if (payment.getStatus() == PaymentStatus.PENDING) {
                payment.setStatus(PaymentStatus.CANCELLED);
                if (payment.getCheckoutSessionId() != null) {
                    expireCheckoutAfterCommit(payment.getCheckoutSessionId());
                }
            } else if (payment.getStatus() == PaymentStatus.PAID) {
                loyaltyService.onPaidOrderCancelled(order);
                if (payment.getPaymentMethod() == PaymentMethod.INSTALLMENT && !partnerPaid) {
                    payment.setStatus(PaymentStatus.CANCELLED);
                } else {
                    payment.setStatus(PaymentStatus.REFUND_PENDING);
                    log.info("Order id={} cancelled after payment id={} was paid: waiting for a refund",
                            order.getId(), payment.getId());
                }
            }
        });
        if (plan != null && (plan.getStatus() == InstallmentStatus.PENDING_APPROVAL
                || plan.getStatus() == InstallmentStatus.APPROVED || plan.getStatus() == InstallmentStatus.SETTLED)) {
            plan.setStatus(InstallmentStatus.CANCELLED);
            if (plan.getPartner() != null) {
                providers.forPartner(plan.getPartner().getCode()).cancel(plan);
            }
        }
    }

    /** Staff (or the bank webhook) saw the transfer arrive: the order counts for points now. COD is paid on delivery. */
    public void confirmTransfer(Order order, User staff, String transactionId, LocalDateTime now) {
        Payment payment = mainPayment(order);
        if (payment.getPaymentMethod() != PaymentMethod.BANK_TRANSFER) {
            throw new BusinessException(ErrorCode.INVALID_PAYMENT_STATUS,
                    "Chỉ xác nhận nhận tiền cho đơn chuyển khoản; đơn COD được ghi nhận đã thu tiền khi giao hàng");
        }
        requirePaymentStatus(payment, order, PaymentStatus.PENDING);
        payment.setStatus(PaymentStatus.PAID);
        payment.setPaidAt(now);
        payment.setConfirmedBy(staff);
        payment.setTransactionId(trimToNull(transactionId));
        loyaltyService.onPaid(order);
        mailPublisher.paymentReceived(order);
    }

    /** Staff sent the money of a cancelled, already paid order back to the customer. */
    public void confirmRefund(Order order, User staff, LocalDateTime now) {
        Payment payment = mainPayment(order);
        requirePaymentStatus(payment, order, PaymentStatus.REFUND_PENDING);
        payment.setStatus(PaymentStatus.REFUNDED);
        payment.setRefundedAt(now);
        payment.setRefundedBy(staff);
    }

    /**
     * The partner answered the application. Approved: the fee and the net amount are fixed, the order counts as paid
     * and the customer is mailed. Rejected: the order stays pending, the customer may choose another partner or
     * another payment method. A repeated event (same {@code eventId}) is ignored and returns false.
     *
     * @param staff the staff member using the partner console, null when the partner itself answered
     */
    public boolean applyPartnerDecision(Order order, boolean approved, String reason, User staff, String eventId,
                                        LocalDateTime now) {
        InstallmentOrder application = application(order);
        if (eventId != null && eventId.equals(application.getLastEventId())) {
            return false;
        }
        if (application.getStatus() != InstallmentStatus.PENDING_APPROVAL || application.getPartner() == null) {
            throw new BusinessException(ErrorCode.INVALID_INSTALLMENT_STATUS,
                    "Hồ sơ trả góp của đơn %s không còn chờ đối tác trả lời (hiện tại: %s)"
                            .formatted(OrderMapper.code(order.getId()), application.getStatus()));
        }
        if (order.getStatus() != OrderStatus.PENDING) {
            throw new BusinessException(ErrorCode.INVALID_INSTALLMENT_STATUS,
                    "Đơn %s không còn ở trạng thái chờ xử lý".formatted(OrderMapper.code(order.getId())));
        }
        application.setLastEventId(eventId);
        application.setReviewedBy(staff);
        application.setReviewedAt(now);
        if (approved) {
            InstallmentFeePolicy.Fee fee = feePolicy.fee(application.getPartner().getCode(), application.getNumMonths(),
                    order.getTotalAmount());
            application.setStatus(InstallmentStatus.APPROVED);
            application.setFeePercent(fee.percent());
            application.setFeeAmount(fee.amount());
            application.setNetAmount(fee.net());
            application.setRejectionReason(null);
            Payment payment = mainPayment(order);
            requirePaymentStatus(payment, order, PaymentStatus.PENDING);
            payment.setStatus(PaymentStatus.PAID);
            payment.setPaidAt(now);
            payment.setConfirmedBy(staff);
            payment.setTransactionId(application.getPartnerRef());
            loyaltyService.onPaid(order);
            mailPublisher.paymentReceived(order);
        } else {
            application.setStatus(InstallmentStatus.REJECTED);
            application.setRejectionReason(trimToNull(reason) != null ? reason.trim()
                    : "Đối tác không chấp thuận hồ sơ trả góp");
        }
        return true;
    }

    /** The customer sends a rejected application to another partner (or the same one with other terms). */
    public void resubmit(Order order, InstallmentRequest request) {
        InstallmentOrder application = application(order);
        if (application.getStatus() != InstallmentStatus.REJECTED || order.getStatus() != OrderStatus.PENDING) {
            throw new BusinessException(ErrorCode.INVALID_INSTALLMENT_STATUS,
                    "Chỉ gửi lại được hồ sơ trả góp đã bị đối tác từ chối của đơn đang chờ xử lý");
        }
        InstallmentPartner partner = partnerPolicy.resolve(request);
        partnerPolicy.checkAmount(partner, order.getTotalAmount());
        fill(application, partner, request, order.getTotalAmount());
        application.setAttemptCount(application.getAttemptCount() + 1);
        application.setRejectionReason(null);
        application.setReviewedAt(null);
        application.setReviewedBy(null);
        application.setLastEventId(null);
        autoDecider.scheduleAfterCommit(order.getId());
    }

    /** The partner paid the shop the net amount: only after the order was delivered. */
    public void settle(Order order, User staff, LocalDateTime now) {
        InstallmentOrder application = application(order);
        if (application.getStatus() != InstallmentStatus.APPROVED || application.getPartner() == null) {
            throw new BusinessException(ErrorCode.INVALID_INSTALLMENT_STATUS,
                    "Chỉ ghi nhận đối tác đã chuyển tiền cho hồ sơ đã được duyệt (hiện tại: %s)".formatted(application.getStatus()));
        }
        if (order.getStatus() != OrderStatus.DELIVERED) {
            throw new BusinessException(ErrorCode.INVALID_INSTALLMENT_STATUS,
                    "Đối tác chỉ chuyển tiền sau khi đơn %s đã giao".formatted(OrderMapper.code(order.getId())));
        }
        application.setStatus(InstallmentStatus.SETTLED);
        application.setSettledAt(now);
        application.setSettledBy(staff);
    }

    /** Sets the partner, term, citizen id and card bank of an application and sends it to the partner. */
    private void fill(InstallmentOrder application, InstallmentPartner partner, InstallmentRequest request,
                      BigDecimal orderTotal) {
        application.setPartner(partner);
        application.setNumMonths(request.months());
        application.setMonthlyPayment(InstallmentPolicy.monthlyPayment(orderTotal, request.months()));
        application.setCitizenId(request.citizenId().trim());
        application.setCardBank(request.cardBank());
        application.setStatus(InstallmentStatus.PENDING_APPROVAL);
        application.setPartnerRef(providers.forPartner(partner.getCode()).submit(application));
    }

    /**
     * Closes the Stripe page of a cancelled order once the cancellation is saved, so it can no longer be paid. Best
     * effort: if Stripe cannot be reached, a late payment arrives as REFUND_PENDING (CardPaymentService).
     */
    private void expireCheckoutAfterCommit(String sessionId) {
        Runnable expire = () -> {
            try {
                stripeGateway.expireSession(sessionId);
            } catch (RuntimeException e) {
                log.warn("Could not expire Stripe session {}: {}", sessionId, e.getMessage());
            }
        };
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    expire.run();
                }
            });
        } else {
            expire.run();
        }
    }

    private Payment mainPayment(Order order) {
        return paymentRepository.findByOrderIdAndInstallmentPaymentIsNull(order.getId())
                .orElseThrow(() -> new BusinessException(ErrorCode.PAYMENT_NOT_FOUND,
                        "Đơn %s không có khoản thanh toán chính".formatted(OrderMapper.code(order.getId()))));
    }

    private InstallmentOrder application(Order order) {
        return installmentOrderRepository.findByOrderId(order.getId())
                .orElseThrow(() -> new BusinessException(ErrorCode.INSTALLMENT_NOT_FOUND,
                        "Đơn %s không phải đơn trả góp".formatted(OrderMapper.code(order.getId()))));
    }

    private static void requirePaymentStatus(Payment payment, Order order, PaymentStatus expected) {
        if (payment.getStatus() != expected) {
            throw new BusinessException(ErrorCode.INVALID_PAYMENT_STATUS,
                    "Khoản thanh toán của đơn %s đang ở trạng thái %s"
                            .formatted(OrderMapper.code(order.getId()), payment.getStatus()));
        }
    }

    private static String trimToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
