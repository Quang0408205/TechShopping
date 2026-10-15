package com.example.Tech.entity.payment;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;

/** One period of an installment plan, created when the order is delivered (due date = delivery date + N months). */
@Entity
@Table(name = "installment_payments")
@Getter
@Setter
@NoArgsConstructor
public class InstallmentPayment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "installment_payment_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "installment_id", nullable = false)
    private InstallmentOrder installment;

    /** 1-based; unique per plan. */
    @Column(name = "payment_number", nullable = false)
    private Integer paymentNumber;

    @Column(name = "amount", precision = 15, scale = 2, nullable = false)
    private BigDecimal amount;

    @Column(name = "due_date", nullable = false)
    private LocalDate dueDate;

    @Column(name = "paid_date")
    private LocalDate paidDate;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", length = 50)
    private InstallmentPaymentStatus status = InstallmentPaymentStatus.PENDING;

    public boolean isOverdue(LocalDate today) {
        return status == InstallmentPaymentStatus.PENDING && dueDate.isBefore(today);
    }
}
