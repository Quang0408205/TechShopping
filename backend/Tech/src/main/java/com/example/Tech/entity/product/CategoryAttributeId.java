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
 * Composite primary key of category_attributes (category_id, attribute_id).
 */
@Embeddable
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode
public class CategoryAttributeId implements Serializable {

    @Column(name = "category_id")
    private Integer categoryId;

    @Column(name = "attribute_id")
    private Integer attributeId;
}
