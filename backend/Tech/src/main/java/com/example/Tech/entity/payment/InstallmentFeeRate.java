package com.example.Tech.entity.payment;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.io.Serializable;
import java.math.BigDecimal;

/** The fee a partner keeps (percent of the order total) for one term. */
@Entity
@Table(name = "installment_fee_rates")
@Getter
@Setter
@NoArgsConstructor
public class InstallmentFeeRate {

    @EmbeddedId
    private Key id;

    @Column(name = "fee_percent", precision = 5, scale = 2, nullable = false)
    private BigDecimal feePercent;

    public InstallmentFeeRate(String partnerCode, int numMonths, BigDecimal feePercent) {
        this.id = new Key(partnerCode, numMonths);
        this.feePercent = feePercent;
    }

    public int getNumMonths() {
        return id.getNumMonths();
    }

    @Embeddable
    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    @EqualsAndHashCode
    public static class Key implements Serializable {

        @Column(name = "partner_code", length = 30)
        private String partnerCode;

        @Column(name = "num_months")
        private int numMonths;
    }
}
