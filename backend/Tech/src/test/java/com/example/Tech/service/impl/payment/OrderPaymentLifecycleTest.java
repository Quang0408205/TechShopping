package com.example.Tech.service.impl.payment;

import com.example.Tech.dto.request.payment.InstallmentRequest;
import com.example.Tech.entity.order.Order;
import com.example.Tech.entity.order.PaymentMethod;
import com.example.Tech.entity.payment.InstallmentBank;
import com.example.Tech.entity.payment.InstallmentOrder;
import com.example.Tech.entity.payment.InstallmentPayment;
import com.example.Tech.entity.payment.InstallmentPaymentStatus;
import com.example.Tech.entity.payment.InstallmentStatus;
import com.example.Tech.entity.payment.Payment;
import com.example.Tech.entity.payment.PaymentStatus;
import com.example.Tech.entity.user.User;
import com.example.Tech.exception.BusinessException;
import com.example.Tech.exception.ErrorCode;
import com.example.Tech.repository.payment.InstallmentOrderRepository;
import com.example.Tech.repository.payment.InstallmentPaymentRepository;
import com.example.Tech.repository.payment.PaymentRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OrderPaymentLifecycleTest {

    private static final InstallmentRequest SIX_MONTHS = new InstallmentRequest(6, " 0123456789 ", InstallmentBank.MB);
    private static final LocalDateTime NOW = LocalDateTime.of(2026, 10, 3, 10, 0);

    @Mock
    private PaymentRepository paymentRepository;

    @Mock
    private InstallmentOrderRepository installmentOrderRepository;

    @Mock
    private InstallmentPaymentRepository installmentPaymentRepository;

    private OrderPaymentLifecycle lifecycle;

    private final User staff = new User();

    @BeforeEach
    void setUp() {
        lifecycle = new OrderPaymentLifecycle(paymentRepository, installmentOrderRepository, installmentPaymentRepository);
    }

    @Test
    void checkCanConfirm_bankTransferMustBePaid_installmentMustBeApproved_codAlwaysOk() {
        Order transfer = order(PaymentMethod.BANK_TRANSFER, "990000");
        when(paymentRepository.findByOrderIdAndInstallmentPaymentIsNull(5L))
                .thenReturn(Optional.of(payment(PaymentMethod.BANK_TRANSFER, PaymentStatus.PENDING)));
        assertThatThrownBy(() -> lifecycle.checkCanConfirm(transfer))
                .hasMessage("Đơn DH00000005 chưa được xác nhận đã nhận tiền chuyển khoản")
                .extracting(ex -> ((BusinessException) ex).getErrorCode())
                .isEqualTo(ErrorCode.PAYMENT_REQUIRED);
        when(paymentRepository.findByOrderIdAndInstallmentPaymentIsNull(5L))
                .thenReturn(Optional.of(payment(PaymentMethod.BANK_TRANSFER, PaymentStatus.PAID)));
        assertThatCode(() -> lifecycle.checkCanConfirm(transfer)).doesNotThrowAnyException();

        Order installment = order(PaymentMethod.INSTALLMENT, "9000000");
        when(installmentOrderRepository.findByOrderId(5L)).thenReturn(Optional.of(plan(InstallmentStatus.PENDING_APPROVAL, 3)));
        assertThatThrownBy(() -> lifecycle.checkCanConfirm(installment))
                .extracting(ex -> ((BusinessException) ex).getErrorCode())
                .isEqualTo(ErrorCode.INSTALLMENT_NOT_APPROVED);
        when(installmentOrderRepository.findByOrderId(5L)).thenReturn(Optional.of(plan(InstallmentStatus.APPROVED, 3)));
        assertThatCode(() -> lifecycle.checkCanConfirm(installment)).doesNotThrowAnyException();

        assertThatCode(() -> lifecycle.checkCanConfirm(order(PaymentMethod.COD, "990000"))).doesNotThrowAnyException();
    }

    @Test
    void onOrderDelivered_cod_isPaidByTheStaffMemberWhoDelivered() {
        Payment payment = payment(PaymentMethod.COD, PaymentStatus.PENDING);
        when(paymentRepository.findByOrderIdAndInstallmentPaymentIsNull(5L)).thenReturn(Optional.of(payment));

        lifecycle.onOrderDelivered(order(PaymentMethod.COD, "990000"), staff, NOW);

        assertThat(payment.getStatus()).isEqualTo(PaymentStatus.PAID);
        assertThat(payment.getPaidAt()).isEqualTo(NOW);
        assertThat(payment.getConfirmedBy()).isSameAs(staff);
    }

    @Test
    void onOrderDelivered_approvedInstallment_createsTheScheduleFromTheDeliveryDate_andStarts() {
        InstallmentOrder plan = plan(InstallmentStatus.APPROVED, 3);
        when(installmentOrderRepository.findByOrderId(5L)).thenReturn(Optional.of(plan));

        lifecycle.onOrderDelivered(order(PaymentMethod.INSTALLMENT, "10000000"), staff, LocalDateTime.of(2027, 1, 31, 15, 0));

        ArgumentCaptor<InstallmentPayment> captor = ArgumentCaptor.forClass(InstallmentPayment.class);
        verify(installmentPaymentRepository, times(3)).save(captor.capture());
        assertThat(captor.getAllValues())
                .extracting(p -> p.getPaymentNumber() + " " + p.getAmount().toPlainString() + " " + p.getDueDate()
                        + " " + p.getStatus())
                .containsExactly(
                        "1 3333333 2027-02-28 PENDING",
                        "2 3333333 2027-03-31 PENDING",
                        "3 3333334 2027-04-30 PENDING");
        assertThat(captor.getAllValues()).allSatisfy(p -> assertThat(p.getInstallment()).isSameAs(plan));
        assertThat(plan.getStatus()).isEqualTo(InstallmentStatus.ACTIVE);
    }

    @Test
    void confirmTransfer_pendingTransferBecomesPaid_codAndPaidAreRefused() {
        Order order = order(PaymentMethod.BANK_TRANSFER, "990000");
        Payment payment = payment(PaymentMethod.BANK_TRANSFER, PaymentStatus.PENDING);
        when(paymentRepository.findByOrderIdAndInstallmentPaymentIsNull(5L)).thenReturn(Optional.of(payment));

        lifecycle.confirmTransfer(order, staff, "  FT1 ", NOW);

        assertThat(payment.getStatus()).isEqualTo(PaymentStatus.PAID);
        assertThat(payment.getPaidAt()).isEqualTo(NOW);
        assertThat(payment.getConfirmedBy()).isSameAs(staff);
        assertThat(payment.getTransactionId()).isEqualTo("FT1");
        assertThatThrownBy(() -> lifecycle.confirmTransfer(order, staff, null, NOW))
                .extracting(ex -> ((BusinessException) ex).getErrorCode())
                .isEqualTo(ErrorCode.INVALID_PAYMENT_STATUS);

        when(paymentRepository.findByOrderIdAndInstallmentPaymentIsNull(5L))
                .thenReturn(Optional.of(payment(PaymentMethod.COD, PaymentStatus.PENDING)));
        assertThatThrownBy(() -> lifecycle.confirmTransfer(order, staff, null, NOW))
                .extracting(ex -> ((BusinessException) ex).getErrorCode())
                .isEqualTo(ErrorCode.INVALID_PAYMENT_STATUS);

        when(paymentRepository.findByOrderIdAndInstallmentPaymentIsNull(5L)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> lifecycle.confirmTransfer(order, staff, null, NOW))
                .extracting(ex -> ((BusinessException) ex).getErrorCode())
                .isEqualTo(ErrorCode.PAYMENT_NOT_FOUND);
    }

    @Test
    void confirmRefund_onlyFromRefundPending() {
        Order order = order(PaymentMethod.BANK_TRANSFER, "990000");
        Payment payment = payment(PaymentMethod.BANK_TRANSFER, PaymentStatus.REFUND_PENDING);
        when(paymentRepository.findByOrderIdAndInstallmentPaymentIsNull(5L)).thenReturn(Optional.of(payment));

        lifecycle.confirmRefund(order, staff, NOW);

        assertThat(payment.getStatus()).isEqualTo(PaymentStatus.REFUNDED);
        assertThat(payment.getRefundedAt()).isEqualTo(NOW);
        assertThat(payment.getRefundedBy()).isSameAs(staff);
        assertThatThrownBy(() -> lifecycle.confirmRefund(order, staff, NOW))
                .extracting(ex -> ((BusinessException) ex).getErrorCode())
                .isEqualTo(ErrorCode.INVALID_PAYMENT_STATUS);
    }

    @Test
    void approveAndReject_onlyAPlanWaitingForApproval() {
        Order order = order(PaymentMethod.INSTALLMENT, "9000000");
        InstallmentOrder approved = plan(InstallmentStatus.PENDING_APPROVAL, 3);
        when(installmentOrderRepository.findByOrderId(5L)).thenReturn(Optional.of(approved));
        lifecycle.approveInstallment(order, staff, NOW);
        assertThat(approved.getStatus()).isEqualTo(InstallmentStatus.APPROVED);
        assertThat(approved.getReviewedBy()).isSameAs(staff);
        assertThat(approved.getReviewedAt()).isEqualTo(NOW);
        assertThatThrownBy(() -> lifecycle.rejectInstallment(order, staff, "x", NOW))
                .extracting(ex -> ((BusinessException) ex).getErrorCode())
                .isEqualTo(ErrorCode.INVALID_INSTALLMENT_STATUS);

        InstallmentOrder rejected = plan(InstallmentStatus.PENDING_APPROVAL, 3);
        when(installmentOrderRepository.findByOrderId(5L)).thenReturn(Optional.of(rejected));
        lifecycle.rejectInstallment(order, staff, "  Sai CCCD ", NOW);
        assertThat(rejected.getStatus()).isEqualTo(InstallmentStatus.REJECTED);
        assertThat(rejected.getRejectionReason()).isEqualTo("Sai CCCD");

        when(installmentOrderRepository.findByOrderId(5L)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> lifecycle.approveInstallment(order, staff, NOW))
                .extracting(ex -> ((BusinessException) ex).getErrorCode())
                .isEqualTo(ErrorCode.INSTALLMENT_NOT_FOUND);
    }

    @Test
    void payPeriod_recordsTheEarliestUnpaidPeriod_withAPaidPayment_lastOneCompletesThePlan() {
        Order order = order(PaymentMethod.INSTALLMENT, "6000000");
        InstallmentOrder plan = plan(InstallmentStatus.ACTIVE, 2);
        plan.setOrder(order);
        InstallmentPayment first = period(plan, 1);
        InstallmentPayment second = period(plan, 2);
        when(installmentPaymentRepository.findAllByInstallmentIdOrderByPaymentNumber(plan.getId()))
                .thenReturn(List.of(first, second));

        assertThatThrownBy(() -> lifecycle.payPeriod(plan, 2, staff, null, NOW))
                .hasMessage("Kỳ tiếp theo cần ghi nhận là kỳ 1")
                .extracting(ex -> ((BusinessException) ex).getErrorCode())
                .isEqualTo(ErrorCode.INSTALLMENT_PERIOD_OUT_OF_ORDER);

        lifecycle.payPeriod(plan, 1, staff, " KY1 ", NOW);
        assertThat(first.getStatus()).isEqualTo(InstallmentPaymentStatus.PAID);
        assertThat(first.getPaidDate()).isEqualTo(NOW.toLocalDate());
        assertThat(plan.getStatus()).isEqualTo(InstallmentStatus.ACTIVE);
        ArgumentCaptor<Payment> captor = ArgumentCaptor.forClass(Payment.class);
        verify(paymentRepository).save(captor.capture());
        Payment payment = captor.getValue();
        assertThat(payment.getOrder()).isSameAs(order);
        assertThat(payment.getAmount()).isEqualByComparingTo("3000000");
        assertThat(payment.getPaymentMethod()).isEqualTo(PaymentMethod.INSTALLMENT);
        assertThat(payment.getStatus()).isEqualTo(PaymentStatus.PAID);
        assertThat(payment.getInstallmentPayment()).isSameAs(first);
        assertThat(payment.getConfirmedBy()).isSameAs(staff);
        assertThat(payment.getTransactionId()).isEqualTo("KY1");

        lifecycle.payPeriod(plan, 2, staff, null, NOW);
        assertThat(plan.getStatus()).isEqualTo(InstallmentStatus.COMPLETED);
        assertThatThrownBy(() -> lifecycle.payPeriod(plan, 2, staff, null, NOW))
                .extracting(ex -> ((BusinessException) ex).getErrorCode())
                .isEqualTo(ErrorCode.INVALID_INSTALLMENT_STATUS);
    }

    @Test
    void payPeriod_planNotStarted_isRefused() {
        assertThatThrownBy(() -> lifecycle.payPeriod(plan(InstallmentStatus.APPROVED, 3), 1, staff, null, NOW))
                .extracting(ex -> ((BusinessException) ex).getErrorCode())
                .isEqualTo(ErrorCode.INVALID_INSTALLMENT_STATUS);
        verify(paymentRepository, never()).save(any());
    }

    private static Payment payment(PaymentMethod method, PaymentStatus status) {
        Payment payment = new Payment();
        payment.setPaymentMethod(method);
        payment.setStatus(status);
        payment.setAmount(new BigDecimal("990000"));
        return payment;
    }

    private static InstallmentOrder plan(InstallmentStatus status, int months) {
        InstallmentOrder plan = new InstallmentOrder();
        plan.setId(3L);
        plan.setStatus(status);
        plan.setNumMonths(months);
        return plan;
    }

    private static InstallmentPayment period(InstallmentOrder plan, int number) {
        InstallmentPayment period = new InstallmentPayment();
        period.setInstallment(plan);
        period.setPaymentNumber(number);
        period.setAmount(new BigDecimal("3000000"));
        period.setDueDate(NOW.toLocalDate().plusMonths(number));
        return period;
    }

    @Test
    void validateSelection_installmentDataOnlyWithInstallment() {
        assertThatCode(() -> lifecycle.validateSelection(PaymentMethod.COD, null)).doesNotThrowAnyException();
        assertThatCode(() -> lifecycle.validateSelection(PaymentMethod.INSTALLMENT, SIX_MONTHS)).doesNotThrowAnyException();

        assertThat(fieldError(() -> lifecycle.validateSelection(PaymentMethod.BANK_TRANSFER, SIX_MONTHS)))
                .containsOnlyKeys("installment");
        assertThat(fieldError(() -> lifecycle.validateSelection(PaymentMethod.INSTALLMENT, null)))
                .containsOnlyKeys("installment");
        assertThat(fieldError(() -> lifecycle.validateSelection(PaymentMethod.INSTALLMENT,
                new InstallmentRequest(5, "0123456789", InstallmentBank.MB))))
                .containsEntry("installment.months", "Kỳ hạn trả góp phải là 3, 6, 9 hoặc 12 tháng");
    }

    @Test
    void checkEligible_installmentNeedsThreeMillion_otherMethodsAnyTotal() {
        assertThatCode(() -> lifecycle.checkEligible(PaymentMethod.INSTALLMENT, new BigDecimal("3000000")))
                .doesNotThrowAnyException();
        assertThatCode(() -> lifecycle.checkEligible(PaymentMethod.COD, new BigDecimal("30000")))
                .doesNotThrowAnyException();
        assertThatThrownBy(() -> lifecycle.checkEligible(PaymentMethod.INSTALLMENT, new BigDecimal("2990000")))
                .extracting(ex -> ((BusinessException) ex).getErrorCode())
                .isEqualTo(ErrorCode.INSTALLMENT_NOT_ELIGIBLE);
    }

    @Test
    void onOrderPlaced_codOrTransfer_createsAPendingMainPaymentOfTheTotal() {
        Order order = order(PaymentMethod.BANK_TRANSFER, "25990000");

        lifecycle.onOrderPlaced(order, null);

        ArgumentCaptor<Payment> captor = ArgumentCaptor.forClass(Payment.class);
        verify(paymentRepository).save(captor.capture());
        Payment payment = captor.getValue();
        assertThat(payment.getOrder()).isSameAs(order);
        assertThat(payment.getAmount()).isEqualByComparingTo("25990000");
        assertThat(payment.getPaymentMethod()).isEqualTo(PaymentMethod.BANK_TRANSFER);
        assertThat(payment.getStatus()).isEqualTo(PaymentStatus.PENDING);
        assertThat(payment.getInstallmentPayment()).isNull();
        verify(installmentOrderRepository, never()).save(any());
    }

    @Test
    void onOrderPlaced_installment_createsAPlanWaitingForApproval_noPayment() {
        Order order = order(PaymentMethod.INSTALLMENT, "10000000");

        lifecycle.onOrderPlaced(order, SIX_MONTHS);

        ArgumentCaptor<InstallmentOrder> captor = ArgumentCaptor.forClass(InstallmentOrder.class);
        verify(installmentOrderRepository).save(captor.capture());
        InstallmentOrder plan = captor.getValue();
        assertThat(plan.getOrder()).isSameAs(order);
        assertThat(plan.getNumMonths()).isEqualTo(6);
        assertThat(plan.getMonthlyPayment()).isEqualByComparingTo("1666666");
        assertThat(plan.getCitizenId()).isEqualTo("0123456789");
        assertThat(plan.getCardBank()).isEqualTo(InstallmentBank.MB);
        assertThat(plan.getStatus()).isEqualTo(InstallmentStatus.PENDING_APPROVAL);
        assertThat(plan.getInterestRate()).isEqualByComparingTo("0");
        verify(paymentRepository, never()).save(any());
    }

    @ParameterizedTest
    @CsvSource({
            "PENDING, CANCELLED",
            "PAID, REFUND_PENDING",
            "REFUNDED, REFUNDED",
            "CANCELLED, CANCELLED"
    })
    void onOrderCancelled_updatesTheMainPayment(PaymentStatus before, PaymentStatus after) {
        Order order = order(PaymentMethod.BANK_TRANSFER, "990000");
        Payment payment = new Payment();
        payment.setStatus(before);
        when(paymentRepository.findByOrderIdAndInstallmentPaymentIsNull(5L)).thenReturn(Optional.of(payment));
        when(installmentOrderRepository.findByOrderId(5L)).thenReturn(Optional.empty());

        lifecycle.onOrderCancelled(order);

        assertThat(payment.getStatus()).isEqualTo(after);
    }

    @ParameterizedTest
    @CsvSource({
            "PENDING_APPROVAL, CANCELLED",
            "APPROVED, CANCELLED",
            "REJECTED, REJECTED"
    })
    void onOrderCancelled_cancelsAPlanThatHasNotStarted(InstallmentStatus before, InstallmentStatus after) {
        Order order = order(PaymentMethod.INSTALLMENT, "9000000");
        InstallmentOrder plan = new InstallmentOrder();
        plan.setStatus(before);
        when(paymentRepository.findByOrderIdAndInstallmentPaymentIsNull(5L)).thenReturn(Optional.empty());
        when(installmentOrderRepository.findByOrderId(5L)).thenReturn(Optional.of(plan));

        lifecycle.onOrderCancelled(order);

        assertThat(plan.getStatus()).isEqualTo(after);
    }

    private static Map<String, String> fieldError(Runnable call) {
        try {
            call.run();
        } catch (BusinessException ex) {
            assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.VALIDATION_ERROR);
            return ex.getDetails();
        }
        throw new AssertionError("Expected a VALIDATION_ERROR");
    }

    private static Order order(PaymentMethod method, String total) {
        Order order = new Order();
        order.setId(5L);
        order.setPaymentMethod(method);
        order.setTotalAmount(new BigDecimal(total));
        return order;
    }
}
