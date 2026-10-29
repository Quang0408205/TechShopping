package com.example.Tech.entity.product;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** One normalised filter value of a product (group + label), rebuilt from its specifications (plan v5 stage 9). */
@Entity
@Table(name = "product_filter_values")
@Getter
@Setter
@NoArgsConstructor
public class ProductFilterValue {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "filter_id")
    private Long id;

    @Column(name = "product_id", nullable = false)
    private Long productId;

    @Column(name = "group_key", length = 30, nullable = false)
    private String groupKey;

    @Column(name = "label", length = 60, nullable = false)
    private String label;

    @Column(name = "sort_order", nullable = false)
    private int sortOrder;
}
