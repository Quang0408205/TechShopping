package com.example.Tech.entity.product;

import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.MapsId;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Link between a product variant and an attribute value (join table without extra columns).
 */
@Entity
@Table(name = "variant_attribute_values")
@Getter
@Setter
@NoArgsConstructor
public class VariantAttributeValue {

    @EmbeddedId
    private VariantAttributeValueId id;

    @MapsId("variantId")
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "variant_id", nullable = false)
    private ProductVariant variant;

    @MapsId("valueId")
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "value_id", nullable = false)
    private AttributeValue attributeValue;

    public VariantAttributeValue(ProductVariant variant, AttributeValue attributeValue) {
        this.id = new VariantAttributeValueId(variant.getId(), attributeValue.getId());
        this.variant = variant;
        this.attributeValue = attributeValue;
    }
}
