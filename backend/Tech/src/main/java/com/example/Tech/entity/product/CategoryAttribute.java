package com.example.Tech.entity.product;

import jakarta.persistence.Column;
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
 * Which attributes a category uses (to generate the product form and the catalogue filter), whether each
 * is required and filterable, and in what order to show it. Schema only for now (v3 survey, 2026-10-05);
 * no API reads/writes this yet.
 */
@Entity
@Table(name = "category_attributes")
@Getter
@Setter
@NoArgsConstructor
public class CategoryAttribute {

    @EmbeddedId
    private CategoryAttributeId id;

    @MapsId("categoryId")
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "category_id", nullable = false)
    private Category category;

    @MapsId("attributeId")
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "attribute_id", nullable = false)
    private Attribute attribute;

    @Column(name = "is_required", nullable = false)
    private Boolean required = true;

    @Column(name = "is_filterable", nullable = false)
    private Boolean filterable = true;

    @Column(name = "display_order")
    private Integer displayOrder;

    public CategoryAttribute(Category category, Attribute attribute, Boolean required, Boolean filterable, Integer displayOrder) {
        this.id = new CategoryAttributeId(category.getId(), attribute.getId());
        this.category = category;
        this.attribute = attribute;
        this.required = required;
        this.filterable = filterable;
        this.displayOrder = displayOrder;
    }
}
