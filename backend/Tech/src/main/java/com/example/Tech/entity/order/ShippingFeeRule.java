package com.example.Tech.entity.order;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/** The home-delivery fee of one province, set by an ADMIN; provinces without a rule pay the default fee. */
@Entity
@Table(name = "shipping_fee_rules")
@Getter
@Setter
@NoArgsConstructor
public class ShippingFeeRule {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "rule_id")
    private Integer id;

    @Column(name = "province_name", length = 100, nullable = false)
    private String provinceName;

    @Column(name = "fee", precision = 15, scale = 2, nullable = false)
    private BigDecimal fee;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}
