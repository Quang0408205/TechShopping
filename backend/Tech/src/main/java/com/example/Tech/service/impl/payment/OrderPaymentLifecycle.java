package com.example.Tech.service.impl.payment;

import com.example.Tech.dto.request.payment.InstallmentRequest;
import com.example.Tech.entity.order.Order;
import com.example.Tech.entity.order.PaymentMethod;
import com.example.Tech.entity.payment.InstallmentOrder;
import com.example.Tech.entity.payment.InstallmentPayment;
import com.example.Tech.entity.payment.InstallmentPaymentStatus;
import com.example.Tech.entity.payment.InstallmentStatus;
import com.example.Tech.entity.payment.Payment;
import com.example.Tech.entity.payment.PaymentStatus;
import com.example.Tech.entity.user.User;
import com.example.Tech.exception.BusinessException;
import com.example.Tech.exception.ErrorCode;
import com.example.Tech.mapper.order.OrderMapper;
import com.example.Tech.repository.payment.InstallmentOrderRepository;
import com.example.Tech.repository.payment.InstallmentPaymentRepository;
import com.example.Tech.repository.payment.PaymentRepository;
import com.example.Tech.service.payment.InstallmentPolicy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

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
    private final InstallmentPaymentRepository installmentPaymentRepository;

    /** Checks that installment data comes with INSTALLMENT and only with it; called before the cart is read. */
    public void validateSelection(PaymentMethod method, InstallmentRequest installment) {
        if (method != PaymentMethod.INSTALLMENT) {
            if (installment != null) {
                throw BusinessException.invalidField("installment", "Chỉ gửi thông tin trả góp khi chọn thanh toán trả góp");
            }
            return;
        }
        if (installment == null) {
            throw BusinessException.invalidField("installment", "Vui lòng nhập thông tin trả góp");
        }
        if (!InstallmentPolicy.isAllowedTerm(installment.months())) {
            throw BusinessException.invalidField("installment.months", "Kỳ hạn trả góp phải là 3, 6, 9 hoặc 12 tháng");
        }
    }

    /** Installment needs an order total of at least {@link InstallmentPolicy#MIN_ORDER_TOTAL}. */
    public void checkEligible(PaymentMethod method, BigDecimal orderTotal) {
        if (method == PaymentMethod.INSTALLMENT && !InstallmentPolicy.isEligible(orderTotal)) {
            throw new BusinessException(ErrorCode.INSTALLMENT_NOT_ELIGIBLE);
        }
    }

    /** COD / BANK_TRANSFER: one PENDING main payment of the total; INSTALLMENT: a plan waiting for approval. */
    public void onOrderPlaced(Order order, InstallmentRequest installment) {
        if (order.getPaymentMethod() == PaymentMethod.INSTALLMENT) {
            InstallmentOrder plan = new InstallmentOrder();
            plan.setOrder(order);
            plan.setNumMonths(installment.months());
            plan.setMonthlyPayment(InstallmentPolicy.monthlyPayment(order.getTotalAmount(), installment.months()));
            plan.setCitizenId(installment.citizenId().trim());
            plan.setCardBank(installment.cardBank());
            installmentOrderRepository.save(plan);
            return;
        }
        Payment payment = new Payment();
        payment.setOrder(order);
        payment.setAmount(order.getTotalAmount());
        payment.setPaymentMethod(order.getPaymentMethod());
        paymentRepository.save(payment);
    }

    /** PENDING → CONFIRMED: a bank transfer must be paid, an installment plan approved. */
    public void checkCanConfirm(Order order) {
        String code = OrderMapper.code(order.getId());
        if (order.getPaymentMethod() == PaymentMethod.BANK_TRANSFER) {
            boolean paid = paymentRepository.findByOrderIdAndInstallmentPaymentIsNull(order.getId())
                    .map(payment -> payment.getStatus() == PaymentStatus.PAID)
                    .orElse(false);
            if (!paid) {
                throw new BusinessException(ErrorCode.PAYMENT_REQUIRED,
                        "Đơn %s chưa được xác nhận đã nhận tiền chuyển khoản".formatted(code));
            }
        } else if (order.getPaymentMethod() == PaymentMethod.INSTALLMENT) {
            boolean approved = installmentOrderRepository.findByOrderId(order.getId())
                    .map(plan -> plan.getStatus() == InstallmentStatus.APPROVED)
                    .orElse(false);
            if (!approved) {
                throw new BusinessException(ErrorCode.INSTALLMENT_NOT_APPROVED,
                        "Hợp đồng trả góp của đơn %s chưa được duyệt".formatted(code));
            }
        }
    }

    /** Delivered: COD money is collected by the courier; an installment plan starts (schedule from today). */
    public void onOrderDelivered(Order order, User staff, LocalDateTime now) {
        if (order.getPaymentMethod() == PaymentMethod.COD) {
            paymentRepository.findByOrderIdAndInstallmentPaymentIsNull(order.getId())
                    .filter(payment -> payment.getStatus() == PaymentStatus.PENDING)
                    .ifPresent(payment -> {
                        payment.setStatus(PaymentStatus.PAID);
                        payment.setPaidAt(now);
                        payment.setConfirmedBy(staff);
                    });
        } else if (order.getPaymentMethod() == PaymentMethod.INSTALLMENT) {
            installmentOrderRepository.findByOrderId(order.getId())
                    .filter(plan -> plan.getStatus() == InstallmentStatus.APPROVED)
                    .ifPresent(plan -> startSchedule(plan, order, now.toLocalDate()));
        }
    }

    /**
     * The order was just cancelled: an awaited payment is cancelled, money already received waits for a refund,
     * a plan that was not started yet is cancelled.
     */
    public void onOrderCancelled(Order order) {
        paymentRepository.findByOrderIdAndInstallmentPaymentIsNull(order.getId()).ifPresent(payment -> {
            if (payment.getStatus() == PaymentStatus.PENDING) {
                payment.setStatus(PaymentStatus.CANCELLED);
            } else if (payment.getStatus() == PaymentStatus.PAID) {
                payment.setStatus(PaymentStatus.REFUND_PENDING);
                log.info("Order id={} cancelled after payment id={} was paid: waiting for a refund",
                        order.getId(), payment.getId());
            }
        });
        installmentOrderRepository.findByOrderId(order.getId()).ifPresent(plan -> {
            if (plan.getStatus() == InstallmentStatus.PENDING_APPROVAL || plan.getStatus() == InstallmentStatus.APPROVED) {
                plan.setStatus(InstallmentStatus.CANCELLED);
            }
        });
    }

    /** Staff saw the bank transfer arrive. COD is never confirmed by hand: it is paid on delivery. */
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
    }

    /** Staff sent the money of a cancelled, already paid order back to the customer. */
    public void confirmRefund(Order order, User staff, LocalDateTime now) {
        Payment payment = mainPayment(order);
        requirePaymentStatus(payment, order, PaymentStatus.REFUND_PENDING);
        payment.setStatus(PaymentStatus.REFUNDED);
        payment.setRefundedAt(now);
        payment.setRefundedBy(staff);
    }

    public void approveInstallment(Order order, User staff, LocalDateTime now) {
        InstallmentOrder plan = planWaitingForApproval(order);
        plan.setStatus(InstallmentStatus.APPROVED);
        plan.setReviewedBy(staff);
        plan.setReviewedAt(now);
    }

    /** Rejects the plan; the caller cancels the order. */
    public void rejectInstallment(Order order, User staff, String reason, LocalDateTime now) {
        InstallmentOrder plan = planWaitingForApproval(order);
        plan.setStatus(InstallmentStatus.REJECTED);
        plan.setRejectionReason(reason.trim());
        plan.setReviewedBy(staff);
        plan.setReviewedAt(now);
    }

    /**
     * Collects period {@code number} of an ACTIVE plan: it must be the earliest unpaid one, paid in full. Adds a PAID
     * payment linked to the period; the last period completes the plan. The caller has locked the plan's order.
     */
    public void payPeriod(InstallmentOrder plan, int number, User staff, String transactionId, LocalDateTime now) {
        if (plan.getStatus() != InstallmentStatus.ACTIVE) {
            throw new BusinessException(ErrorCode.INVALID_INSTALLMENT_STATUS,
                    "Chỉ ghi nhận kỳ trả góp khi hợp đồng đang thực hiện (hiện tại: %s)".formatted(plan.getStatus()));
        }
        List<InstallmentPayment> periods = installmentPaymentRepository.findAllByInstallmentIdOrderByPaymentNumber(plan.getId());
        InstallmentPayment next = periods.stream()
                .filter(period -> period.getStatus() == InstallmentPaymentStatus.PENDING)
                .findFirst()
                .orElseThrow(() -> new BusinessException(ErrorCode.INVALID_INSTALLMENT_STATUS,
                        "Hợp đồng trả góp đã thanh toán đủ các kỳ"));
        if (next.getPaymentNumber() != number) {
            throw new BusinessException(ErrorCode.INSTALLMENT_PERIOD_OUT_OF_ORDER,
                    "Kỳ tiếp theo cần ghi nhận là kỳ %d".formatted(next.getPaymentNumber()));
        }
        next.setStatus(InstallmentPaymentStatus.PAID);
        next.setPaidDate(now.toLocalDate());

        Payment payment = new Payment();
        payment.setOrder(plan.getOrder());
        payment.setAmount(next.getAmount());
        payment.setPaymentMethod(PaymentMethod.INSTALLMENT);
        payment.setStatus(PaymentStatus.PAID);
        payment.setPaidAt(now);
        payment.setConfirmedBy(staff);
        payment.setTransactionId(trimToNull(transactionId));
        payment.setInstallmentPayment(next);
        paymentRepository.save(payment);

        if (next.getPaymentNumber() == periods.size()) {
            plan.setStatus(InstallmentStatus.COMPLETED);
            log.info("Installment plan id={} completed", plan.getId());
        }
    }

    private void startSchedule(InstallmentOrder plan, Order order, LocalDate deliveredOn) {
        List<BigDecimal> amounts = InstallmentPolicy.periodAmounts(order.getTotalAmount(), plan.getNumMonths());
        for (int number = 1; number <= amounts.size(); number++) {
            InstallmentPayment period = new InstallmentPayment();
            period.setInstallment(plan);
            period.setPaymentNumber(number);
            period.setAmount(amounts.get(number - 1));
            period.setDueDate(InstallmentPolicy.dueDate(deliveredOn, number));
            installmentPaymentRepository.save(period);
        }
        plan.setStatus(InstallmentStatus.ACTIVE);
        log.info("Installment plan id={} of order id={} started: {} period(s)", plan.getId(), order.getId(), amounts.size());
    }

    private Payment mainPayment(Order order) {
        return paymentRepository.findByOrderIdAndInstallmentPaymentIsNull(order.getId())
                .orElseThrow(() -> new BusinessException(ErrorCode.PAYMENT_NOT_FOUND,
                        "Đơn %s không có khoản thanh toán COD / chuyển khoản".formatted(OrderMapper.code(order.getId()))));
    }

    private InstallmentOrder planWaitingForApproval(Order order) {
        InstallmentOrder plan = installmentOrderRepository.findByOrderId(order.getId())
                .orElseThrow(() -> new BusinessException(ErrorCode.INSTALLMENT_NOT_FOUND,
                        "Đơn %s không phải đơn trả góp".formatted(OrderMapper.code(order.getId()))));
        if (plan.getStatus() != InstallmentStatus.PENDING_APPROVAL) {
            throw new BusinessException(ErrorCode.INVALID_INSTALLMENT_STATUS,
                    "Hợp đồng trả góp của đơn %s không còn chờ duyệt (hiện tại: %s)"
                            .formatted(OrderMapper.code(order.getId()), plan.getStatus()));
        }
        return plan;
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
