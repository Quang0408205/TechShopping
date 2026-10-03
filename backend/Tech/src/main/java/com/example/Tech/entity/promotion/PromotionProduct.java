package com.example.Tech.entity.promotion;

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
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * A product selected into a promotion, with an optional per-product override discount. Mapped as an
 * entity (not @ManyToMany) because the join table carries discount_type/discount_value: null on both
 * means "use the campaign's default discount" (see Promotion.discountType/discountValue).
 */
@Entity
@Table(name = "promotion_products")
@Getter
@Setter
@NoArgsConstructor
public class PromotionProduct {

    @EmbeddedId
    private PromotionProductId id;

    @MapsId("promotionId")
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "promotion_id", nullable = false)
    private Promotion promotion;

    @MapsId("productId")
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    @Enumerated(EnumType.STRING)
    @Column(name = "discount_type", length = 20)
    private DiscountType discountType;

    @Column(name = "discount_value", precision = 15, scale = 2)
    private BigDecimal discountValue;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    public PromotionProduct(Promotion promotion, Product product, DiscountType discountType, BigDecimal discountValue) {
        this.id = new PromotionProductId(promotion.getId(), product.getId());
        this.promotion = promotion;
        this.product = product;
        this.discountType = discountType;
        this.discountValue = discountValue;
    }

    /** Whether this row overrides the campaign's default discount. */
    public boolean hasOverride() {
        return discountType != null && discountValue != null;
    }
}
