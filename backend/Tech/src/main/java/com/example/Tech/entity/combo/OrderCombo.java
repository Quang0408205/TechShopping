package com.example.Tech.entity.combo;

import com.example.Tech.entity.order.Order;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
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

/** A combo applied to an order: the name is copied, so renaming the combo later does not change old orders. */
@Entity
@Table(name = "order_combos")
@Getter
@Setter
@NoArgsConstructor
public class OrderCombo {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "order_combo_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "order_id", nullable = false)
    private Order order;

    @Column(name = "combo_id", nullable = false)
    private Long comboId;

    @Column(name = "combo_name", length = 150, nullable = false)
    private String comboName;

    /** Add-on units that got the discount. */
    @Column(name = "units", nullable = false)
    private Integer units;

    @Column(name = "discount_amount", precision = 15, scale = 2, nullable = false)
    private BigDecimal discountAmount;
}
