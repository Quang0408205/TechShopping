package com.example.Tech.service.impl.payment;

import com.example.Tech.config.StripeProperties;
import com.example.Tech.dto.request.payment.InstallmentRequest;
import com.example.Tech.entity.order.Order;
import com.example.Tech.entity.order.OrderStatus;
import com.example.Tech.entity.order.PaymentMethod;
import com.example.Tech.entity.payment.InstallmentBank;
import com.example.Tech.entity.payment.InstallmentFeeRate;
import com.example.Tech.entity.payment.InstallmentOrder;
import com.example.Tech.entity.payment.InstallmentPartner;
import com.example.Tech.entity.payment.InstallmentPartnerType;
import com.example.Tech.entity.payment.InstallmentStatus;
import com.example.Tech.entity.payment.Payment;
import com.example.Tech.entity.payment.PaymentStatus;
import com.example.Tech.entity.user.User;
import com.example.Tech.exception.BusinessException;
import com.example.Tech.exception.ErrorCode;
import com.example.Tech.repository.payment.InstallmentFeeRateRepository;
import com.example.Tech.repository.payment.InstallmentOrderRepository;
import com.example.Tech.repository.payment.InstallmentPartnerRepository;
import com.example.Tech.repository.payment.PaymentRepository;
import com.example.Tech.service.mail.OrderMailPublisher;
import com.example.Tech.service.payment.StripeGateway;
import com.example.Tech.service.payment.installment.CreditCardInstallmentProvider;
import com.example.Tech.service.payment.installment.FinanceInstallmentProvider;
import com.example.Tech.service.payment.installment.InstallmentAutoDecider;
import com.example.Tech.service.payment.installment.InstallmentFeePolicy;
import com.example.Tech.service.payment.installment.InstallmentPartnerPolicy;
import com.example.Tech.service.payment.installment.InstallmentProviderRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

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
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class OrderPaymentLifecycleTest {

    private static final InstallmentRequest SIX_MONTHS_CARD =
            new InstallmentRequest(6, " 0123456789 ", InstallmentBank.MB, "CREDIT_CARD");
    private static final LocalDateTime NOW = LocalDateTime.of(2026, 10, 3, 10, 0);

    @Mock
    private PaymentRepository paymentRepository;

    @Mock
    private InstallmentOrderRepository installmentOrderRepository;

    @Mock
    private InstallmentPartnerRepository partnerRepository;

    @Mock
    private InstallmentFeeRateRepository feeRateRepository;

    @Mock
    private InstallmentAutoDecider autoDecider;

    @Mock
    private StripeGateway stripeGateway;

    @Mock
    private OrderMailPublisher mailPublisher;

    private final InstallmentPartner card = partner("CREDIT_CARD", InstallmentPartnerType.CREDIT_CARD, "3000000", null);
    private final InstallmentPartner financeB = partner("FINANCE_B", InstallmentPartnerType.FINANCE_COMPANY, "3000000", "15000000");
    private final User staff = new User();

    private OrderPaymentLifecycle lifecycle;

    @BeforeEach
    void setUp() {
        lifecycle = lifecycle(new StripeProperties("sk_test_x", null, null, null));
        when(partnerRepository.findById("CREDIT_CARD")).thenReturn(Optional.of(card));
        when(partnerRepository.findById("FINANCE_B")).thenReturn(Optional.of(financeB));
        when(feeRateRepository.findOne("CREDIT_CARD", 6)).thenReturn(Optional.of(rate("CREDIT_CARD", 6, "3")));
        when(feeRateRepository.findOne("CREDIT_CARD", 3)).thenReturn(Optional.of(rate("CREDIT_CARD", 3, "2")));
        when(feeRateRepository.findOne("FINANCE_B", 3)).thenReturn(Optional.of(rate("FINANCE_B", 3, "3")));
    }

    private OrderPaymentLifecycle lifecycle(StripeProperties stripe) {
        InstallmentPartnerPolicy partnerPolicy = new InstallmentPartnerPolicy(partnerRepository, feeRateRepository);
        InstallmentProviderRegistry registry = new InstallmentProviderRegistry(
                List.of(new CreditCardInstallmentProvider(), new FinanceInstallmentProvider()));
        return new OrderPaymentLifecycle(paymentRepository, installmentOrderRepository, partnerPolicy,
                new InstallmentFeePolicy(feeRateRepository), registry, autoDecider, stripe, stripeGateway, mailPublisher);
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
        for (InstallmentStatus waiting : List.of(InstallmentStatus.PENDING_APPROVAL, InstallmentStatus.REJECTED)) {
            when(installmentOrderRepository.findByOrderId(5L)).thenReturn(Optional.of(application(waiting)));
            assertThatThrownBy(() -> lifecycle.checkCanConfirm(installment))
                    .extracting(ex -> ((BusinessException) ex).getErrorCode())
                    .isEqualTo(ErrorCode.INSTALLMENT_NOT_APPROVED);
        }
        for (InstallmentStatus answered : List.of(InstallmentStatus.APPROVED, InstallmentStatus.SETTLED)) {
            when(installmentOrderRepository.findByOrderId(5L)).thenReturn(Optional.of(application(answered)));
            assertThatCode(() -> lifecycle.checkCanConfirm(installment)).doesNotThrowAnyException();
        }

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
    void onOrderDelivered_installment_changesNothing_thePartnerCollectsFromTheCustomer() {
        lifecycle.onOrderDelivered(order(PaymentMethod.INSTALLMENT, "10000000"), staff, NOW);

        verify(paymentRepository, never()).findByOrderIdAndInstallmentPaymentIsNull(any());
        verify(installmentOrderRepository, never()).findByOrderId(any());
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
    void partnerApproval_fixesTheFee_paysTheMainPayment_andMailsTheCustomer() {
        Order order = order(PaymentMethod.INSTALLMENT, "30000000");
        InstallmentOrder application = application(InstallmentStatus.PENDING_APPROVAL);
        application.setPartnerRef("CC-1");
        Payment payment = payment(PaymentMethod.INSTALLMENT, PaymentStatus.PENDING);
        when(installmentOrderRepository.findByOrderId(5L)).thenReturn(Optional.of(application));
        when(paymentRepository.findByOrderIdAndInstallmentPaymentIsNull(5L)).thenReturn(Optional.of(payment));

        boolean applied = lifecycle.applyPartnerDecision(order, true, null, staff, "evt-1", NOW);

        assertThat(applied).isTrue();
        assertThat(application.getStatus()).isEqualTo(InstallmentStatus.APPROVED);
        assertThat(application.getFeePercent()).isEqualByComparingTo("3");
        assertThat(application.getFeeAmount()).isEqualByComparingTo("900000");
        assertThat(application.getNetAmount()).isEqualByComparingTo("29100000");
        assertThat(application.getReviewedBy()).isSameAs(staff);
        assertThat(application.getReviewedAt()).isEqualTo(NOW);
        assertThat(application.getLastEventId()).isEqualTo("evt-1");
        assertThat(payment.getStatus()).isEqualTo(PaymentStatus.PAID);
        assertThat(payment.getPaidAt()).isEqualTo(NOW);
        assertThat(payment.getTransactionId()).isEqualTo("CC-1");
        verify(mailPublisher).paymentReceived(order);
    }

    @Test
    void partnerAnswer_repeatedEventIsIgnored_otherAnswersOnlyWhileWaiting() {
        Order order = order(PaymentMethod.INSTALLMENT, "9000000");
        InstallmentOrder application = application(InstallmentStatus.APPROVED);
        application.setLastEventId("evt-1");
        when(installmentOrderRepository.findByOrderId(5L)).thenReturn(Optional.of(application));

        assertThat(lifecycle.applyPartnerDecision(order, true, null, null, "evt-1", NOW)).isFalse();
        assertThatThrownBy(() -> lifecycle.applyPartnerDecision(order, false, "x", null, "evt-2", NOW))
                .extracting(ex -> ((BusinessException) ex).getErrorCode())
                .isEqualTo(ErrorCode.INVALID_INSTALLMENT_STATUS);

        when(installmentOrderRepository.findByOrderId(5L)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> lifecycle.applyPartnerDecision(order, true, null, null, "evt-3", NOW))
                .extracting(ex -> ((BusinessException) ex).getErrorCode())
                .isEqualTo(ErrorCode.INSTALLMENT_NOT_FOUND);
    }

    @Test
    void partnerRejection_keepsTheOrderAndTheMainPaymentPending_withTheReason() {
        Order order = order(PaymentMethod.INSTALLMENT, "9000000");
        InstallmentOrder application = application(InstallmentStatus.PENDING_APPROVAL);
        when(installmentOrderRepository.findByOrderId(5L)).thenReturn(Optional.of(application));

        lifecycle.applyPartnerDecision(order, false, "  Sai CCCD ", null, "evt-1", NOW);

        assertThat(application.getStatus()).isEqualTo(InstallmentStatus.REJECTED);
        assertThat(application.getRejectionReason()).isEqualTo("Sai CCCD");
        assertThat(application.getFeeAmount()).isNull();
        assertThat(order.getStatus()).isEqualTo(OrderStatus.PENDING);
        verify(paymentRepository, never()).findByOrderIdAndInstallmentPaymentIsNull(any());
        verify(mailPublisher, never()).paymentReceived(any());
    }

    @Test
    void resubmit_onlyARejectedApplication_goesToTheNewPartner_andCountsTheAttempt() {
        Order order = order(PaymentMethod.INSTALLMENT, "9000000");
        InstallmentOrder application = application(InstallmentStatus.REJECTED);
        application.setRejectionReason("no");
        when(installmentOrderRepository.findByOrderId(5L)).thenReturn(Optional.of(application));

        lifecycle.resubmit(order, new InstallmentRequest(3, "0123456781", null, "FINANCE_B"));

        assertThat(application.getStatus()).isEqualTo(InstallmentStatus.PENDING_APPROVAL);
        assertThat(application.getPartner()).isSameAs(financeB);
        assertThat(application.getNumMonths()).isEqualTo(3);
        assertThat(application.getMonthlyPayment()).isEqualByComparingTo("3000000");
        assertThat(application.getCardBank()).isNull();
        assertThat(application.getRejectionReason()).isNull();
        assertThat(application.getAttemptCount()).isEqualTo(2);
        assertThat(application.getPartnerRef()).startsWith("FB-");
        verify(autoDecider).scheduleAfterCommit(5L);

        InstallmentOrder waiting = application(InstallmentStatus.PENDING_APPROVAL);
        when(installmentOrderRepository.findByOrderId(5L)).thenReturn(Optional.of(waiting));
        assertThatThrownBy(() -> lifecycle.resubmit(order, SIX_MONTHS_CARD))
                .extracting(ex -> ((BusinessException) ex).getErrorCode())
                .isEqualTo(ErrorCode.INVALID_INSTALLMENT_STATUS);
    }

    @Test
    void resubmit_theNewPartnerMustAcceptTheOrderTotal() {
        Order order = order(PaymentMethod.INSTALLMENT, "20000000");
        when(installmentOrderRepository.findByOrderId(5L)).thenReturn(Optional.of(application(InstallmentStatus.REJECTED)));

        assertThatThrownBy(() -> lifecycle.resubmit(order, new InstallmentRequest(3, "0123456781", null, "FINANCE_B")))
                .extracting(ex -> ((BusinessException) ex).getErrorCode())
                .isEqualTo(ErrorCode.INSTALLMENT_NOT_ELIGIBLE);
    }

    @Test
    void settle_onlyAnApprovedApplicationOfADeliveredOrder() {
        Order order = order(PaymentMethod.INSTALLMENT, "9000000");
        InstallmentOrder application = application(InstallmentStatus.APPROVED);
        when(installmentOrderRepository.findByOrderId(5L)).thenReturn(Optional.of(application));

        assertThatThrownBy(() -> lifecycle.settle(order, staff, NOW))
                .extracting(ex -> ((BusinessException) ex).getErrorCode())
                .isEqualTo(ErrorCode.INVALID_INSTALLMENT_STATUS);

        order.setStatus(OrderStatus.DELIVERED);
        lifecycle.settle(order, staff, NOW);
        assertThat(application.getStatus()).isEqualTo(InstallmentStatus.SETTLED);
        assertThat(application.getSettledAt()).isEqualTo(NOW);
        assertThat(application.getSettledBy()).isSameAs(staff);

        assertThatThrownBy(() -> lifecycle.settle(order, staff, NOW))
                .extracting(ex -> ((BusinessException) ex).getErrorCode())
                .isEqualTo(ErrorCode.INVALID_INSTALLMENT_STATUS);
    }

    @Test
    void card_isRefusedWhenStripeIsOff_orAboveStripesLimit() {
        OrderPaymentLifecycle off = lifecycle(new StripeProperties("", null, null, null));

        assertThatThrownBy(() -> off.validateSelection(PaymentMethod.CARD, null))
                .isInstanceOfSatisfying(BusinessException.class,
                        ex -> assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.CARD_PAYMENT_DISABLED));
        assertThatCode(() -> lifecycle.validateSelection(PaymentMethod.CARD, null)).doesNotThrowAnyException();
        assertThatCode(() -> lifecycle.checkEligible(PaymentMethod.CARD, new BigDecimal("99999999"), null)).doesNotThrowAnyException();
        assertThatThrownBy(() -> lifecycle.checkEligible(PaymentMethod.CARD, new BigDecimal("100000000"), null))
                .isInstanceOfSatisfying(BusinessException.class,
                        ex -> assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.CARD_AMOUNT_TOO_LARGE));
    }

    @Test
    void cardOrder_isConfirmedOnlyOncePaid() {
        Order order = order(PaymentMethod.CARD, "990000");
        when(paymentRepository.findByOrderIdAndInstallmentPaymentIsNull(5L))
                .thenReturn(Optional.of(payment(PaymentMethod.CARD, PaymentStatus.PENDING)));

        assertThatThrownBy(() -> lifecycle.checkCanConfirm(order))
                .isInstanceOfSatisfying(BusinessException.class, ex -> {
                    assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.PAYMENT_REQUIRED);
                    assertThat(ex.getMessage()).contains("thẻ");
                });

        when(paymentRepository.findByOrderIdAndInstallmentPaymentIsNull(5L))
                .thenReturn(Optional.of(payment(PaymentMethod.CARD, PaymentStatus.PAID)));
        assertThatCode(() -> lifecycle.checkCanConfirm(order)).doesNotThrowAnyException();
    }

    @Test
    void cancellingAnUnpaidCardOrder_closesItsStripePage() {
        Payment payment = payment(PaymentMethod.CARD, PaymentStatus.PENDING);
        payment.setCheckoutSessionId("cs_test_1");
        when(paymentRepository.findByOrderIdAndInstallmentPaymentIsNull(5L)).thenReturn(Optional.of(payment));
        when(installmentOrderRepository.findByOrderId(5L)).thenReturn(Optional.empty());

        lifecycle.onOrderCancelled(order(PaymentMethod.CARD, "990000"));

        assertThat(payment.getStatus()).isEqualTo(PaymentStatus.CANCELLED);
        verify(stripeGateway).expireSession("cs_test_1");
    }

    @Test
    void validateSelection_installmentDataOnlyWithInstallment_andFitsThePartner() {
        assertThatCode(() -> lifecycle.validateSelection(PaymentMethod.COD, null)).doesNotThrowAnyException();
        assertThatCode(() -> lifecycle.validateSelection(PaymentMethod.INSTALLMENT, SIX_MONTHS_CARD)).doesNotThrowAnyException();
        assertThatCode(() -> lifecycle.validateSelection(PaymentMethod.INSTALLMENT,
                new InstallmentRequest(6, "0123456789", InstallmentBank.MB))).doesNotThrowAnyException();

        assertThat(fieldError(() -> lifecycle.validateSelection(PaymentMethod.BANK_TRANSFER, SIX_MONTHS_CARD)))
                .containsOnlyKeys("installment");
        assertThat(fieldError(() -> lifecycle.validateSelection(PaymentMethod.INSTALLMENT, null)))
                .containsOnlyKeys("installment");
        assertThat(fieldError(() -> lifecycle.validateSelection(PaymentMethod.INSTALLMENT,
                new InstallmentRequest(5, "0123456789", InstallmentBank.MB, "CREDIT_CARD"))))
                .containsEntry("installment.months", "Thẻ tín dụng không hỗ trợ kỳ hạn 5 tháng");
        assertThat(fieldError(() -> lifecycle.validateSelection(PaymentMethod.INSTALLMENT,
                new InstallmentRequest(6, "0123456789", null, "CREDIT_CARD"))))
                .containsOnlyKeys("installment.cardBank");
        assertThat(fieldError(() -> lifecycle.validateSelection(PaymentMethod.INSTALLMENT,
                new InstallmentRequest(3, "0123456789", InstallmentBank.MB, "FINANCE_B"))))
                .containsOnlyKeys("installment.cardBank");
        assertThat(fieldError(() -> lifecycle.validateSelection(PaymentMethod.INSTALLMENT,
                new InstallmentRequest(3, "0123456789", null, "NO_SUCH_PARTNER"))))
                .containsOnlyKeys("installment.partner");
        assertThat(fieldError(() -> lifecycle.validateSelection(PaymentMethod.INSTALLMENT,
                new InstallmentRequest(3, "0123456789", null, null))))
                .containsOnlyKeys("installment.partner");
    }

    @Test
    void checkEligible_installmentMustFitThePartnersRange_otherMethodsAnyTotal() {
        InstallmentRequest financeRequest = new InstallmentRequest(3, "0123456789", null, "FINANCE_B");
        assertThatCode(() -> lifecycle.checkEligible(PaymentMethod.INSTALLMENT, new BigDecimal("3000000"), SIX_MONTHS_CARD))
                .doesNotThrowAnyException();
        assertThatCode(() -> lifecycle.checkEligible(PaymentMethod.INSTALLMENT, new BigDecimal("15000000"), financeRequest))
                .doesNotThrowAnyException();
        assertThatCode(() -> lifecycle.checkEligible(PaymentMethod.COD, new BigDecimal("30000"), null))
                .doesNotThrowAnyException();
        assertThatThrownBy(() -> lifecycle.checkEligible(PaymentMethod.INSTALLMENT, new BigDecimal("2990000"), SIX_MONTHS_CARD))
                .extracting(ex -> ((BusinessException) ex).getErrorCode())
                .isEqualTo(ErrorCode.INSTALLMENT_NOT_ELIGIBLE);
        assertThatThrownBy(() -> lifecycle.checkEligible(PaymentMethod.INSTALLMENT, new BigDecimal("15000001"), financeRequest))
                .hasMessageContaining("Công ty tài chính B")
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
        verify(autoDecider, never()).scheduleAfterCommit(any());
    }

    @Test
    void onOrderPlaced_installment_sendsTheApplicationToThePartner_andKeepsAPendingMainPayment() {
        Order order = order(PaymentMethod.INSTALLMENT, "10000000");

        lifecycle.onOrderPlaced(order, SIX_MONTHS_CARD);

        ArgumentCaptor<InstallmentOrder> captor = ArgumentCaptor.forClass(InstallmentOrder.class);
        verify(installmentOrderRepository).save(captor.capture());
        InstallmentOrder application = captor.getValue();
        assertThat(application.getOrder()).isSameAs(order);
        assertThat(application.getPartner()).isSameAs(card);
        assertThat(application.getNumMonths()).isEqualTo(6);
        assertThat(application.getMonthlyPayment()).isEqualByComparingTo("1666666");
        assertThat(application.getCitizenId()).isEqualTo("0123456789");
        assertThat(application.getCardBank()).isEqualTo(InstallmentBank.MB);
        assertThat(application.getStatus()).isEqualTo(InstallmentStatus.PENDING_APPROVAL);
        assertThat(application.getInterestRate()).isEqualByComparingTo("0");
        assertThat(application.getPartnerRef()).startsWith("CC-");
        assertThat(application.getAttemptCount()).isEqualTo(1);
        ArgumentCaptor<Payment> payment = ArgumentCaptor.forClass(Payment.class);
        verify(paymentRepository).save(payment.capture());
        assertThat(payment.getValue().getPaymentMethod()).isEqualTo(PaymentMethod.INSTALLMENT);
        assertThat(payment.getValue().getStatus()).isEqualTo(PaymentStatus.PENDING);
        assertThat(payment.getValue().getAmount()).isEqualByComparingTo("10000000");
        verify(autoDecider).scheduleAfterCommit(5L);
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
            "PENDING_APPROVAL, CANCELLED, PENDING, CANCELLED",
            "APPROVED, CANCELLED, PAID, CANCELLED",
            "SETTLED, CANCELLED, PAID, REFUND_PENDING",
            "REJECTED, REJECTED, PENDING, CANCELLED"
    })
    void onOrderCancelled_withdrawsTheApplication_andOnlyOwesARefundOncePartnerPaid(
            InstallmentStatus before, InstallmentStatus after, PaymentStatus paymentBefore, PaymentStatus paymentAfter) {
        Order order = order(PaymentMethod.INSTALLMENT, "9000000");
        InstallmentOrder application = application(before);
        Payment payment = payment(PaymentMethod.INSTALLMENT, paymentBefore);
        when(paymentRepository.findByOrderIdAndInstallmentPaymentIsNull(5L)).thenReturn(Optional.of(payment));
        when(installmentOrderRepository.findByOrderId(5L)).thenReturn(Optional.of(application));

        lifecycle.onOrderCancelled(order);

        assertThat(application.getStatus()).isEqualTo(after);
        assertThat(payment.getStatus()).isEqualTo(paymentAfter);
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

    private static Payment payment(PaymentMethod method, PaymentStatus status) {
        Payment payment = new Payment();
        payment.setPaymentMethod(method);
        payment.setStatus(status);
        payment.setAmount(new BigDecimal("990000"));
        return payment;
    }

    private InstallmentOrder application(InstallmentStatus status) {
        InstallmentOrder application = new InstallmentOrder();
        application.setId(3L);
        application.setStatus(status);
        application.setNumMonths(6);
        application.setPartner(card);
        application.setCitizenId("0123456789");
        return application;
    }

    private static InstallmentPartner partner(String code, InstallmentPartnerType type, String min, String max) {
        InstallmentPartner partner = new InstallmentPartner();
        partner.setCode(code);
        partner.setName(code.equals("CREDIT_CARD") ? "Thẻ tín dụng" : "Công ty tài chính B");
        partner.setType(type);
        partner.setMinOrderTotal(new BigDecimal(min));
        partner.setMaxOrderTotal(max == null ? null : new BigDecimal(max));
        return partner;
    }

    private static InstallmentFeeRate rate(String partner, int months, String percent) {
        return new InstallmentFeeRate(partner, months, new BigDecimal(percent));
    }

    private static Order order(PaymentMethod method, String total) {
        Order order = new Order();
        order.setId(5L);
        order.setPaymentMethod(method);
        order.setTotalAmount(new BigDecimal(total));
        order.setStatus(OrderStatus.PENDING);
        return order;
    }
}
