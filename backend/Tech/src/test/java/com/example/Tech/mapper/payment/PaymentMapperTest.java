package com.example.Tech.mapper.payment;

import com.example.Tech.config.BankTransferProperties;
import com.example.Tech.dto.response.payment.InstallmentResponse;
import com.example.Tech.dto.response.payment.PaymentResponse;
import com.example.Tech.entity.order.Order;
import com.example.Tech.entity.order.PaymentMethod;
import com.example.Tech.entity.payment.InstallmentBank;
import com.example.Tech.entity.payment.InstallmentOrder;
import com.example.Tech.entity.payment.InstallmentPayment;
import com.example.Tech.entity.payment.InstallmentPaymentStatus;
import com.example.Tech.entity.payment.InstallmentStatus;
import com.example.Tech.entity.payment.Payment;
import com.example.Tech.entity.payment.PaymentStatus;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class PaymentMapperTest {

    private final PaymentMapper mapper = new PaymentMapper(
            new BankTransferProperties("Vietcombank", "970436", "0123456789", "CONG TY POY", 24));

    @Test
    void pendingBankTransfer_hasTheTransferInstructionsAndAVietQrImage() {
        Order order = order(42L, PaymentMethod.BANK_TRANSFER, "25990000.00");

        PaymentResponse response = mapper.toResponse(payment(PaymentMethod.BANK_TRANSFER, PaymentStatus.PENDING, "25990000.00"), order);

        assertThat(response.bankTransfer()).isNotNull();
        assertThat(response.bankTransfer().bankName()).isEqualTo("Vietcombank");
        assertThat(response.bankTransfer().accountNumber()).isEqualTo("0123456789");
        assertThat(response.bankTransfer().transferContent()).isEqualTo("DH00000042");
        assertThat(response.bankTransfer().amount()).isEqualByComparingTo("25990000");
        assertThat(response.bankTransfer().payBefore()).isEqualTo(LocalDateTime.of(2026, 10, 4, 10, 0));
        assertThat(response.bankTransfer().qrImageUrl()).isEqualTo(
                "https://img.vietqr.io/image/970436-0123456789-compact2.png"
                        + "?amount=25990000&addInfo=DH00000042&accountName=CONG%20TY%20POY");
    }

    @Test
    void paidTransferAndCod_haveNoTransferInstructions() {
        Order order = order(42L, PaymentMethod.BANK_TRANSFER, "990000");

        assertThat(mapper.toResponse(payment(PaymentMethod.BANK_TRANSFER, PaymentStatus.PAID, "990000"), order).bankTransfer())
                .isNull();
        assertThat(mapper.toResponse(payment(PaymentMethod.COD, PaymentStatus.PENDING, "990000"), order).bankTransfer())
                .isNull();
    }

    @Test
    void installment_masksTheCitizenIdForTheCustomer_countsPaidPeriods_flagsOverdue() {
        Order order = order(7L, PaymentMethod.INSTALLMENT, "10000000");
        InstallmentOrder plan = new InstallmentOrder();
        plan.setId(3L);
        plan.setNumMonths(3);
        plan.setMonthlyPayment(new BigDecimal("3333333"));
        plan.setCitizenId("0123456789");
        plan.setCardBank(InstallmentBank.TCB);
        plan.setStatus(InstallmentStatus.ACTIVE);
        List<InstallmentPayment> periods = List.of(
                period(plan, 1, "3333333", LocalDate.of(2026, 11, 3), InstallmentPaymentStatus.PAID),
                period(plan, 2, "3333333", LocalDate.of(2026, 12, 3), InstallmentPaymentStatus.PENDING),
                period(plan, 3, "3333334", LocalDate.of(2027, 1, 3), InstallmentPaymentStatus.PENDING));
        LocalDate today = LocalDate.of(2026, 12, 10);

        InstallmentResponse customer = mapper.toResponse(plan, order, periods, today, false);
        InstallmentResponse staff = mapper.toResponse(plan, order, periods, today, true);

        assertThat(customer.citizenId()).isEqualTo("******6789");
        assertThat(staff.citizenId()).isEqualTo("0123456789");
        assertThat(customer.cardBankName()).isEqualTo("Techcombank");
        assertThat(customer.lastPayment()).isEqualByComparingTo("3333334");
        assertThat(customer.paidPeriods()).isEqualTo(1);
        assertThat(customer.paidAmount()).isEqualByComparingTo("3333333");
        assertThat(customer.remainingAmount()).isEqualByComparingTo("6666667");
        assertThat(customer.periods()).extracting(p -> p.number() + ":" + p.overdue())
                .containsExactly("1:false", "2:true", "3:false");
    }

    @Test
    void rejectedInstallment_hasNothingRemaining() {
        Order order = order(7L, PaymentMethod.INSTALLMENT, "10000000");
        InstallmentOrder plan = new InstallmentOrder();
        plan.setNumMonths(6);
        plan.setMonthlyPayment(new BigDecimal("1666666"));
        plan.setCitizenId("0123456789");
        plan.setCardBank(InstallmentBank.VCB);
        plan.setStatus(InstallmentStatus.REJECTED);
        plan.setRejectionReason("Không liên lạc được");

        InstallmentResponse response = mapper.toResponse(plan, order, List.of(), LocalDate.of(2026, 10, 3), false);

        assertThat(response.remainingAmount()).isEqualByComparingTo("0");
        assertThat(response.rejectionReason()).isEqualTo("Không liên lạc được");
        assertThat(response.periods()).isEmpty();
    }

    private static Order order(Long id, PaymentMethod method, String total) {
        Order order = new Order();
        order.setId(id);
        order.setPaymentMethod(method);
        order.setTotalAmount(new BigDecimal(total));
        order.setOrderDate(LocalDateTime.of(2026, 10, 3, 10, 0));
        return order;
    }

    private static Payment payment(PaymentMethod method, PaymentStatus status, String amount) {
        Payment payment = new Payment();
        payment.setPaymentMethod(method);
        payment.setStatus(status);
        payment.setAmount(new BigDecimal(amount));
        return payment;
    }

    private static InstallmentPayment period(InstallmentOrder plan, int number, String amount, LocalDate due,
                                             InstallmentPaymentStatus status) {
        InstallmentPayment period = new InstallmentPayment();
        period.setInstallment(plan);
        period.setPaymentNumber(number);
        period.setAmount(new BigDecimal(amount));
        period.setDueDate(due);
        period.setStatus(status);
        return period;
    }
}
