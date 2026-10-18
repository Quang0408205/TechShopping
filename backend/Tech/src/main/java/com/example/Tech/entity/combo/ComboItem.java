package com.example.Tech.entity.combo;

import com.example.Tech.entity.product.Product;
import jakarta.persistence.Column;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.MapsId;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

/** One add-on of a combo: the product and its discount per unit (a percentage, or a fixed amount in đồng). */
@Entity
@Table(name = "combo_items")
@Getter
@Setter
@NoArgsConstructor
public class ComboItem {

    @EmbeddedId
    private ComboItemKey id = new ComboItemKey();

    @MapsId("comboId")
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "combo_id")
    private Combo combo;

    @MapsId("productId")
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "product_id")
    private Product product;

    @Enumerated(EnumType.STRING)
    @Column(name = "discount_type", length = 10, nullable = false)
    private ComboDiscountType discountType;

    @Column(name = "discount_value", precision = 15, scale = 2, nullable = false)
    private BigDecimal discountValue;
}
