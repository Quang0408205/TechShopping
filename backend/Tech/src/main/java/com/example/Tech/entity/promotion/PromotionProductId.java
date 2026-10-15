package com.example.Tech.entity.promotion;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.io.Serializable;

/**
 * Composite primary key of promotion_products (promotion_id, product_id).
 */
@Embeddable
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode
public class PromotionProductId implements Serializable {

    @Column(name = "promotion_id")
    private Long promotionId;

    @Column(name = "product_id")
    private Long productId;
}
