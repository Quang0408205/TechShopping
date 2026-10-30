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
 * The installment application of an INSTALLMENT order (one per order). 0% interest for the customer and no down
 * payment: monthlyPayment is the order total divided by numMonths rounded down. The partner chosen by the customer
 * approves or rejects it and later pays the shop {@code netAmount} = total minus {@code feeAmount}. Rows without a
 * partner are plans from before partners existed: they are only shown, never changed.
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
    @Column(name = "card_bank_code", length = 20)
    private InstallmentBank cardBank;

    /** Null on plans from before partners existed. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "partner_code")
    private InstallmentPartner partner;

    /** Reference the partner gave to the application (used by its webhook). */
    @Column(name = "partner_ref", length = 60)
    private String partnerRef;

    /** Fee, amount and net amount are fixed when the partner approves, so later rate edits do not change them. */
    @Column(name = "fee_percent", precision = 5, scale = 2)
    private BigDecimal feePercent;

    @Column(name = "fee_amount", precision = 15, scale = 2)
    private BigDecimal feeAmount;

    @Column(name = "net_amount", precision = 15, scale = 2)
    private BigDecimal netAmount;

    @Column(name = "settled_at")
    private LocalDateTime settledAt;

    /** Staff member who recorded that the partner paid the shop. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "settled_by")
    private User settledBy;

    /** 1 for the first application; grows each time a rejected application is sent to another partner. */
    @Column(name = "attempt_count", nullable = false)
    private int attemptCount = 1;

    /** Id of the last partner event applied, so a repeated webhook delivery is ignored. */
    @Column(name = "last_event_id", length = 80)
    private String lastEventId;

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
