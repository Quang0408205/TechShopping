package com.example.Tech.mapper.payment;

import com.example.Tech.config.BankTransferProperties;
import com.example.Tech.dto.response.payment.BankTransferResponse;
import com.example.Tech.dto.response.payment.InstallmentPeriodResponse;
import com.example.Tech.dto.response.payment.InstallmentResponse;
import com.example.Tech.dto.response.payment.PaymentResponse;
import com.example.Tech.entity.order.Order;
import com.example.Tech.entity.order.PaymentMethod;
import com.example.Tech.entity.payment.InstallmentOrder;
import com.example.Tech.entity.payment.InstallmentPayment;
import com.example.Tech.entity.payment.InstallmentPaymentStatus;
import com.example.Tech.entity.payment.InstallmentStatus;
import com.example.Tech.entity.payment.Payment;
import com.example.Tech.entity.payment.PaymentStatus;
import com.example.Tech.mapper.order.OrderMapper;
import com.example.Tech.service.payment.InstallmentPolicy;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.List;

@Component
@RequiredArgsConstructor
public class PaymentMapper {

    private static final String VIETQR_IMAGE_URL = "https://img.vietqr.io/image/%s-%s-compact2.png?amount=%s&addInfo=%s&accountName=%s";

    private final BankTransferProperties bankTransfer;

    public PaymentResponse toResponse(Payment payment, Order order) {
        boolean awaitingTransfer = payment.getPaymentMethod() == PaymentMethod.BANK_TRANSFER
                && payment.getStatus() == PaymentStatus.PENDING;
        return new PaymentResponse(
                payment.getId(),
                payment.getStatus(),
                payment.getAmount(),
                payment.getTransactionId(),
                payment.getPaidAt(),
                payment.getRefundedAt(),
                awaitingTransfer ? toBankTransfer(payment, order) : null);
    }

    public InstallmentResponse toResponse(InstallmentOrder plan, Order order, List<InstallmentPayment> periods,
                                          LocalDate today, boolean revealCitizenId) {
        BigDecimal total = order.getTotalAmount();
        List<InstallmentPayment> paid = periods.stream()
                .filter(period -> period.getStatus() == InstallmentPaymentStatus.PAID)
                .toList();
        BigDecimal paidAmount = paid.stream().map(InstallmentPayment::getAmount).reduce(BigDecimal.ZERO, BigDecimal::add);
        boolean closed = plan.getStatus() == InstallmentStatus.REJECTED || plan.getStatus() == InstallmentStatus.CANCELLED;
        return new InstallmentResponse(
                plan.getId(),
                plan.getStatus(),
                plan.getNumMonths(),
                plan.getMonthlyPayment(),
                InstallmentPolicy.lastPayment(total, plan.getNumMonths()),
                total,
                plan.getInterestRate(),
                revealCitizenId ? plan.getCitizenId() : InstallmentPolicy.maskCitizenId(plan.getCitizenId()),
                plan.getCardBank(),
                plan.getCardBank().getDisplayName(),
                plan.getRejectionReason(),
                plan.getReviewedAt(),
                paid.size(),
                paidAmount,
                closed ? BigDecimal.ZERO : total.subtract(paidAmount),
                periods.stream().map(period -> toPeriodResponse(period, today)).toList());
    }

    private BankTransferResponse toBankTransfer(Payment payment, Order order) {
        String code = OrderMapper.code(order.getId());
        String amount = payment.getAmount().setScale(0, RoundingMode.CEILING).toPlainString();
        String qrImageUrl = VIETQR_IMAGE_URL.formatted(encode(bankTransfer.bankBin()), encode(bankTransfer.accountNumber()),
                amount, encode(code), encode(bankTransfer.accountName()));
        return new BankTransferResponse(
                bankTransfer.bankName(),
                bankTransfer.accountNumber(),
                bankTransfer.accountName(),
                payment.getAmount(),
                code,
                qrImageUrl,
                order.getOrderDate() != null ? order.getOrderDate().plusHours(bankTransfer.deadlineHours()) : null);
    }

    private static InstallmentPeriodResponse toPeriodResponse(InstallmentPayment period, LocalDate today) {
        return new InstallmentPeriodResponse(
                period.getId(),
                period.getPaymentNumber(),
                period.getAmount(),
                period.getDueDate(),
                period.getPaidDate(),
                period.getStatus(),
                period.isOverdue(today));
    }

    private static String encode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8).replace("+", "%20");
    }
}
