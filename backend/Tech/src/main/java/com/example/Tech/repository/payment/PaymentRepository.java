package com.example.Tech.repository.payment;

import com.example.Tech.entity.payment.Payment;
import com.example.Tech.entity.order.OrderStatus;
import com.example.Tech.entity.order.PaymentMethod;
import com.example.Tech.entity.payment.PaymentStatus;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface PaymentRepository extends JpaRepository<Payment, Long> {

    /** The main payment of a COD / BANK_TRANSFER order (not an installment period). */
    Optional<Payment> findByOrderIdAndInstallmentPaymentIsNull(Long orderId);

    /** Main payments of a page of orders in one query. */
    List<Payment> findAllByOrderIdInAndInstallmentPaymentIsNull(Collection<Long> orderIds);

    /** Orders of a user, newest first, whose main payment by one of these methods is still awaited. */
    @Query("select p.order.id from Payment p where p.order.user.id = :userId and p.installmentPayment is null "
            + "and p.paymentMethod in :methods and p.status = :paymentStatus and p.order.status = :orderStatus "
            + "and p.order.orderDate >= :since order by p.order.id desc")
    List<Long> findAwaitedOrderIds(Long userId, Collection<PaymentMethod> methods, PaymentStatus paymentStatus,
                                   OrderStatus orderStatus, LocalDateTime since, Pageable pageable);
}
