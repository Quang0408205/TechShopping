package com.example.Tech.repository.payment;

import com.example.Tech.entity.payment.InstallmentPayment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.Collection;
import java.util.List;

public interface InstallmentPaymentRepository extends JpaRepository<InstallmentPayment, Long> {

    List<InstallmentPayment> findAllByInstallmentIdOrderByPaymentNumber(Long installmentId);

    /** Schedules of several plans in one query, grouped by plan then period. */
    @Query("""
            select p from InstallmentPayment p
            where p.installment.id in :installmentIds
            order by p.installment.id, p.paymentNumber
            """)
    List<InstallmentPayment> findAllByInstallmentIdIn(Collection<Long> installmentIds);
}
