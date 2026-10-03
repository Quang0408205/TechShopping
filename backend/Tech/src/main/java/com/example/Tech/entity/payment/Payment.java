package com.example.Tech.entity.payment;

import com.example.Tech.entity.order.Order;
import com.example.Tech.entity.order.PaymentMethod;
import com.example.Tech.entity.user.User;
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
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * A money movement of an order. COD and BANK_TRANSFER orders have one "main" payment (installmentPayment null,
 * unique per order by the partial index uq_payments_order_main); an INSTALLMENT order gets one PAID payment per
 * period collected, linked to that period.
 */
@Entity
@Table(name = "payments")
@Getter
@Setter
@NoArgsConstructor
public class Payment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "payment_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "order_id", nullable = false)
    private Order order;

    @Column(name = "amount", precision = 15, scale = 2, nullable = false)
    private BigDecimal amount;

    @Enumerated(EnumType.STRING)
    @Column(name = "payment_method", length = 50, nullable = false)
    private PaymentMethod paymentMethod;

    /** Bank reference typed by staff (optional). */
    @Column(name = "transaction_id", length = 100)
    private String transactionId;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", length = 50)
    private PaymentStatus status = PaymentStatus.PENDING;

    @Column(name = "paid_at")
    private LocalDateTime paidAt;

    /** Staff member who confirmed the money (for COD: the one who marked the order delivered). */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "confirmed_by")
    private User confirmedBy;

    @Column(name = "refunded_at")
    private LocalDateTime refundedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "refunded_by")
    private User refundedBy;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "installment_payment_id", unique = true)
    private InstallmentPayment installmentPayment;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}
