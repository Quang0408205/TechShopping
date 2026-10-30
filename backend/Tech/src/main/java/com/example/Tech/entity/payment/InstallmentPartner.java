package com.example.Tech.entity.payment;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

/**
 * A company that finances the installment plans: it approves the customer, collects the monthly amounts and pays the
 * shop the order total minus its fee. The terms it supports are the rows of installment_fee_rates.
 */
@Entity
@Table(name = "installment_partners")
@Getter
@Setter
@NoArgsConstructor
public class InstallmentPartner {

    @Id
    @Column(name = "partner_code", length = 30)
    private String code;

    @Column(name = "partner_name", length = 100, nullable = false)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(name = "partner_type", length = 30, nullable = false)
    private InstallmentPartnerType type;

    @Column(name = "min_order_total", precision = 15, scale = 2, nullable = false)
    private BigDecimal minOrderTotal;

    /** Null = no upper limit. */
    @Column(name = "max_order_total", precision = 15, scale = 2)
    private BigDecimal maxOrderTotal;

    @Column(name = "is_active", nullable = false)
    private boolean active = true;

    @Column(name = "display_order", nullable = false)
    private int displayOrder;

    public boolean accepts(BigDecimal orderTotal) {
        return orderTotal != null && orderTotal.compareTo(minOrderTotal) >= 0
                && (maxOrderTotal == null || orderTotal.compareTo(maxOrderTotal) <= 0);
    }
}
