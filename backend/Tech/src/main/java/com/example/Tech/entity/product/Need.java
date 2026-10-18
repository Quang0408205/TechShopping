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

/** A shopping need of a category (e.g. Laptop: Gaming); a product can have several. */
@Entity
@Table(name = "needs")
@Getter
@Setter
@NoArgsConstructor
public class Need {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "need_id")
    private Integer id;

    @Column(name = "category_id", nullable = false)
    private Integer categoryId;

    @Column(name = "slug", length = 60, nullable = false)
    private String slug;

    @Column(name = "name", length = 100, nullable = false)
    private String name;

    @Column(name = "display_order", nullable = false)
    private int displayOrder;
}
