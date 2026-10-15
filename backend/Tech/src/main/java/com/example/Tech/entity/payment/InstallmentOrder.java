package com.example.Tech.entity.payment;

import com.example.Tech.entity.order.Order;
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
 * The installment plan of an INSTALLMENT order (one per order). 0% interest and no down payment: monthlyPayment is
 * the order total divided by numMonths rounded down, the last period takes the remainder.
 */
@Entity
@Table(name = "installment_orders")
@Getter
@Setter
@NoArgsConstructor
public class InstallmentOrder {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "installment_id")
    private Long id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "order_id", nullable = false, unique = true)
    private Order order;

    @Column(name = "num_months", nullable = false)
    private Integer numMonths;

    @Column(name = "monthly_payment", precision = 15, scale = 2, nullable = false)
    private BigDecimal monthlyPayment;

    @Column(name = "interest_rate", precision = 5, scale = 2)
    private BigDecimal interestRate = BigDecimal.ZERO;

    @Column(name = "total_interest", precision = 15, scale = 2)
    private BigDecimal totalInterest = BigDecimal.ZERO;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", length = 50)
    private InstallmentStatus status = InstallmentStatus.PENDING_APPROVAL;

    /** Digits only (DB CHECK); the exact length is checked by the request DTO. */
    @Column(name = "citizen_id", length = 20, nullable = false)
    private String citizenId;

    @Enumerated(EnumType.STRING)
    @Column(name = "card_bank_code", length = 20, nullable = false)
    private InstallmentBank cardBank;

    /** Required by a DB CHECK when the status is REJECTED. */
    @Column(name = "rejection_reason", columnDefinition = "text")
    private String rejectionReason;

    /** Staff member who approved or rejected the plan. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reviewed_by")
    private User reviewedBy;

    @Column(name = "reviewed_at")
    private LocalDateTime reviewedAt;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}
