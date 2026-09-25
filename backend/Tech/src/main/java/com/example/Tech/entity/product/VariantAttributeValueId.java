package com.example.Tech.entity.product;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.io.Serializable;

/**
 * Composite primary key of variant_attribute_values (variant_id, value_id).
 */
@Embeddable
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode
public class VariantAttributeValueId implements Serializable {

    @Column(name = "variant_id")
    private Long variantId;

    @Column(name = "value_id")
    private Integer valueId;
}
