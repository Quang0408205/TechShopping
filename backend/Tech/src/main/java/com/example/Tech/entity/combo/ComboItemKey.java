package com.example.Tech.entity.combo;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

import java.io.Serializable;
import java.util.Objects;

@Embeddable
public class ComboItemKey implements Serializable {

    @Column(name = "combo_id")
    private Long comboId;

    @Column(name = "product_id")
    private Long productId;

    public ComboItemKey() {
    }

    public ComboItemKey(Long comboId, Long productId) {
        this.comboId = comboId;
        this.productId = productId;
    }

    public Long getComboId() {
        return comboId;
    }

    public Long getProductId() {
        return productId;
    }

    @Override
    public boolean equals(Object other) {
        return other instanceof ComboItemKey key && Objects.equals(comboId, key.comboId)
                && Objects.equals(productId, key.productId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(comboId, productId);
    }
}
