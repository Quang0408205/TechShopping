package com.example.Tech.repository.payment;

import com.example.Tech.entity.order.Order;
import com.example.Tech.entity.order.OrderStatus;
import com.example.Tech.entity.order.PaymentMethod;
import com.example.Tech.entity.payment.InstallmentBank;
import com.example.Tech.entity.payment.InstallmentOrder;
import com.example.Tech.entity.payment.InstallmentPayment;
import com.example.Tech.entity.payment.InstallmentPaymentStatus;
import com.example.Tech.entity.payment.InstallmentStatus;
import com.example.Tech.entity.payment.Payment;
import com.example.Tech.entity.payment.PaymentStatus;
import com.example.Tech.entity.user.User;
import com.example.Tech.repository.order.OrderRepository;
import com.example.Tech.repository.user.UserRepository;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Checks the payment group mapping (Phase 5 columns, enums as text, partial unique index, CHECKs) against the real
 * PostgreSQL test database. Every test is rolled back; a test that expects the database to refuse a row does it
 * last, because PostgreSQL aborts the transaction after the error.
 */
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ActiveProfiles("test")
class PaymentRepositoryTest {

    @Autowired
    private PaymentRepository paymentRepository;

    @Autowired
    private InstallmentOrderRepository installmentOrderRepository;

    @Autowired
    private InstallmentPaymentRepository installmentPaymentRepository;

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private EntityManager entityManager;

    private User customer;
    private User staff;

    @BeforeEach
    void setUp() {
        customer = userRepository.save(user("payment-test"));
        staff = userRepository.save(user("payment-test-staff"));
        entityManager.flush();
        entityManager.clear();
    }

    @Test
    void mainPayment_isStoredWithUpperCaseEnums_andFoundByOrder() {
        Order order = order(PaymentMethod.BANK_TRANSFER, "25990000");
        Payment payment = payment(order, PaymentMethod.BANK_TRANSFER, "25990000");
        payment.setStatus(PaymentStatus.PAID);
        payment.setPaidAt(LocalDateTime.of(2026, 10, 3, 9, 30));
        payment.setTransactionId("FT2627600001");
        payment.setConfirmedBy(userRepository.getReferenceById(staff.getId()));
        Long paymentId = paymentRepository.save(payment).getId();
        entityManager.flush();
        entityManager.clear();

        Payment saved = paymentRepository.findByOrderIdAndInstallmentPaymentIsNull(order.getId()).orElseThrow();
        assertThat(saved.getId()).isEqualTo(paymentId);
        assertThat(saved.getStatus()).isEqualTo(PaymentStatus.PAID);
        assertThat(saved.getAmount()).isEqualByComparingTo("25990000");
        assertThat(saved.getConfirmedBy().getId()).isEqualTo(staff.getId());
        assertThat(saved.getCreatedAt()).isNotNull();

        Object[] raw = (Object[]) entityManager
                .createNativeQuery("select status, payment_method from payments where payment_id = :id")
                .setParameter("id", paymentId)
                .getSingleResult();
        assertThat(raw).containsExactly("PAID", "BANK_TRANSFER");
        assertThat(paymentRepository.findByOrderIdAndInstallmentPaymentIsNull(-1L)).isEmpty();
    }

    @Test
    void findAllByOrderIdIn_returnsOnlyTheMainPayments() {
        Order cod = order(PaymentMethod.COD, "990000");
        Order transfer = order(PaymentMethod.BANK_TRANSFER, "1990000");
        Order installmentOrder = order(PaymentMethod.INSTALLMENT, "9000000");
        Long codPayment = paymentRepository.save(payment(cod, PaymentMethod.COD, "990000")).getId();
        Long transferPayment = paymentRepository.save(payment(transfer, PaymentMethod.BANK_TRANSFER, "1990000")).getId();
        InstallmentOrder plan = installmentOrderRepository.save(plan(installmentOrder, 3, "3000000"));
        InstallmentPayment first = installmentPaymentRepository.save(period(plan, 1, "3000000", LocalDate.of(2026, 11, 3)));
        Payment periodPayment = payment(installmentOrder, PaymentMethod.INSTALLMENT, "3000000");
        periodPayment.setInstallmentPayment(first);
        paymentRepository.save(periodPayment);
        entityManager.flush();
        entityManager.clear();

        List<Payment> main = paymentRepository.findAllByOrderIdInAndInstallmentPaymentIsNull(
                List.of(cod.getId(), transfer.getId(), installmentOrder.getId()));

        assertThat(main).extracting(Payment::getId).containsExactlyInAnyOrder(codPayment, transferPayment);
        assertThat(paymentRepository.findByOrderIdAndInstallmentPaymentIsNull(installmentOrder.getId())).isEmpty();
    }

    @Test
    void installmentOrder_defaultsToPendingApproval_andIsFoundByOrder() {
        Order order = order(PaymentMethod.INSTALLMENT, "10000000");
        Long planId = installmentOrderRepository.save(plan(order, 3, "3333333")).getId();
        Order other = order(PaymentMethod.INSTALLMENT, "6000000");
        installmentOrderRepository.save(plan(other, 6, "1000000"));
        entityManager.flush();
        entityManager.clear();

        InstallmentOrder saved = installmentOrderRepository.findByOrderId(order.getId()).orElseThrow();
        assertThat(saved.getId()).isEqualTo(planId);
        assertThat(saved.getStatus()).isEqualTo(InstallmentStatus.PENDING_APPROVAL);
        assertThat(saved.getCardBank()).isEqualTo(InstallmentBank.VCB);
        assertThat(saved.getCitizenId()).isEqualTo("0123456789");
        assertThat(saved.getInterestRate()).isEqualByComparingTo("0");
        assertThat(saved.getTotalInterest()).isEqualByComparingTo("0");

        Object[] raw = (Object[]) entityManager
                .createNativeQuery("select status, card_bank_code from installment_orders where installment_id = :id")
                .setParameter("id", planId)
                .getSingleResult();
        assertThat(raw).containsExactly("PENDING_APPROVAL", "VCB");
        assertThat(installmentOrderRepository.findAllByOrderIdIn(List.of(order.getId(), other.getId(), -1L)))
                .extracting(plan -> plan.getOrder().getId())
                .containsExactlyInAnyOrder(order.getId(), other.getId());
    }

    @Test
    void schedule_isReturnedInPeriodOrder_perPlan_andKnowsWhenItIsOverdue() {
        InstallmentOrder planA = installmentOrderRepository.save(plan(order(PaymentMethod.INSTALLMENT, "9000000"), 3, "3000000"));
        InstallmentOrder planB = installmentOrderRepository.save(plan(order(PaymentMethod.INSTALLMENT, "6000000"), 3, "2000000"));
        installmentPaymentRepository.save(period(planA, 2, "3000000", LocalDate.of(2026, 12, 3)));
        installmentPaymentRepository.save(period(planB, 1, "2000000", LocalDate.of(2026, 11, 5)));
        InstallmentPayment paid = period(planA, 1, "3000000", LocalDate.of(2026, 11, 3));
        paid.setStatus(InstallmentPaymentStatus.PAID);
        paid.setPaidDate(LocalDate.of(2026, 11, 1));
        installmentPaymentRepository.save(paid);
        entityManager.flush();
        entityManager.clear();

        List<InstallmentPayment> scheduleA = installmentPaymentRepository.findAllByInstallmentIdOrderByPaymentNumber(planA.getId());
        assertThat(scheduleA).extracting(InstallmentPayment::getPaymentNumber).containsExactly(1, 2);

        List<InstallmentPayment> both = installmentPaymentRepository.findAllByInstallmentIdIn(
                List.of(planB.getId(), planA.getId()));
        assertThat(both).extracting(p -> p.getInstallment().getId() + "#" + p.getPaymentNumber())
                .containsExactly(planA.getId() + "#1", planA.getId() + "#2", planB.getId() + "#1");

        LocalDate today = LocalDate.of(2026, 12, 4);
        assertThat(scheduleA.get(0).isOverdue(today)).isFalse();
        assertThat(scheduleA.get(1).isOverdue(today)).isTrue();
        assertThat(scheduleA.get(1).isOverdue(LocalDate.of(2026, 12, 3))).isFalse();
    }

    @Test
    void database_refusesASecondMainPaymentForTheSameOrder() {
        Order order = order(PaymentMethod.COD, "990000");
        paymentRepository.save(payment(order, PaymentMethod.COD, "990000"));
        entityManager.flush();

        // IDENTITY ids: save() inserts at once, so the database error comes from save itself
        assertThatThrownBy(() -> paymentRepository.save(payment(order, PaymentMethod.COD, "990000")))
                .hasMessageContaining("uq_payments_order_main");
    }

    @Test
    void database_refusesARejectedPlanWithoutReason() {
        InstallmentOrder plan = plan(order(PaymentMethod.INSTALLMENT, "9000000"), 3, "3000000");
        plan.setStatus(InstallmentStatus.REJECTED);

        assertThatThrownBy(() -> installmentOrderRepository.save(plan))
                .hasMessageContaining("chk_installment_orders_rejection_reason");
    }

    @Test
    void database_refusesACitizenIdWithLetters() {
        InstallmentOrder plan = plan(order(PaymentMethod.INSTALLMENT, "9000000"), 3, "3000000");
        plan.setCitizenId("01234abcde");

        assertThatThrownBy(() -> installmentOrderRepository.save(plan))
                .hasMessageContaining("chk_installment_orders_citizen_id_digits");
    }

    @Test
    void database_refusesAnUnknownPaymentStatus() {
        Order order = order(PaymentMethod.COD, "990000");
        Long paymentId = paymentRepository.save(payment(order, PaymentMethod.COD, "990000")).getId();
        entityManager.flush();

        assertThatThrownBy(() -> entityManager.createNativeQuery("update payments set status = 'pending' where payment_id = :id")
                .setParameter("id", paymentId)
                .executeUpdate())
                .hasMessageContaining("chk_payments_status");
    }

    private Order order(PaymentMethod method, String total) {
        Order order = new Order();
        order.setUser(userRepository.getReferenceById(customer.getId()));
        order.setOrderDate(LocalDateTime.now());
        order.setRecipientName("Nguyễn Văn Nhận");
        order.setRecipientPhone("0901234567");
        order.setShippingAddress("12 Nguyễn Trãi, Quận 5, TP. Hồ Chí Minh");
        order.setTotalAmount(new BigDecimal(total));
        order.setStatus(OrderStatus.PENDING);
        order.setPaymentMethod(method);
        return orderRepository.save(order);
    }

    private static Payment payment(Order order, PaymentMethod method, String amount) {
        Payment payment = new Payment();
        payment.setOrder(order);
        payment.setPaymentMethod(method);
        payment.setAmount(new BigDecimal(amount));
        return payment;
    }

    private static InstallmentOrder plan(Order order, int months, String monthly) {
        InstallmentOrder plan = new InstallmentOrder();
        plan.setOrder(order);
        plan.setNumMonths(months);
        plan.setMonthlyPayment(new BigDecimal(monthly));
        plan.setCitizenId("0123456789");
        plan.setCardBank(InstallmentBank.VCB);
        return plan;
    }

    private static InstallmentPayment period(InstallmentOrder plan, int number, String amount, LocalDate dueDate) {
        InstallmentPayment period = new InstallmentPayment();
        period.setInstallment(plan);
        period.setPaymentNumber(number);
        period.setAmount(new BigDecimal(amount));
        period.setDueDate(dueDate);
        return period;
    }

    private static User user(String username) {
        User user = new User();
        user.setEmail(username + "@techshopping.vn");
        user.setUsername(username);
        user.setPasswordHash("{test}hash");
        user.setFullname("Payment Test");
        return user;
    }
}
