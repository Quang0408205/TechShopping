package com.example.Tech.entity.review;

import com.example.Tech.entity.product.Product;
import com.example.Tech.entity.user.User;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * A 1-5 star rating + comment by one account for one product (Việc 0d, step R1: schema + entity +
 * repository only, 2026-10-05; API/UI are later steps). One review per account per product (DB unique
 * constraint on user_id, product_id). "Đã mua hàng" is never stored here: it is computed at read time
 * from DELIVERED orders. updated_at is NOT @UpdateTimestamp on purpose, matching docs/TechShopping_v2.sql:
 * an admin hide/unhide must not look like "the customer edited this", so only the edit-comment code path
 * (a later step) may set it.
 */
@Entity
@Table(name = "reviews")
@Getter
@Setter
@NoArgsConstructor
public class Review {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "review_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    @Column(name = "rating", nullable = false)
    private Short rating;

    @Column(name = "comment", columnDefinition = "text", nullable = false)
    private String comment;

    @Column(name = "is_hidden", nullable = false)
    private Boolean hidden = false;

    @Column(name = "hidden_reason", columnDefinition = "text")
    private String hiddenReason;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "hidden_by")
    private User hiddenBy;

    @Column(name = "hidden_at")
    private LocalDateTime hiddenAt;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    /** Set by the edit-comment code path only, never by Hibernate (see class note). */
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @OneToMany(mappedBy = "review", cascade = CascadeType.PERSIST)
    @OrderBy("displayOrder")
    private List<ReviewImage> images = new ArrayList<>();

    public void addImage(ReviewImage image) {
        image.setReview(this);
        images.add(image);
    }
}
