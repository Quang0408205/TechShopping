package com.example.Tech.repository.payment;

import com.example.Tech.entity.payment.Payment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface PaymentRepository extends JpaRepository<Payment, Long> {

    /** The main payment of a COD / BANK_TRANSFER order (not an installment period). */
    Optional<Payment> findByOrderIdAndInstallmentPaymentIsNull(Long orderId);

    /** Main payments of a page of orders in one query. */
    List<Payment> findAllByOrderIdInAndInstallmentPaymentIsNull(Collection<Long> orderIds);
}
