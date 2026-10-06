package com.example.Tech.entity.review;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

/**
 * One photo attached to a review. At most 5 per review and no two sharing a display_order: both are
 * checked by application code (a later step), not a DB trigger (the project does not use triggers in any
 * other migration); the DB only enforces the unique(review_id, display_order) pair.
 */
@Entity
@Table(name = "review_images")
@Getter
@Setter
@NoArgsConstructor
public class ReviewImage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "image_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "review_id", nullable = false)
    private Review review;

    /** Only a URL the server itself stored, e.g. /uploads/reviews/<uuid>.jpg (checked by a later step). */
    @Column(name = "image_url", columnDefinition = "text", nullable = false)
    private String imageUrl;

    @Column(name = "display_order", nullable = false)
    private Integer displayOrder;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    public ReviewImage(Review review, String imageUrl, Integer displayOrder) {
        this.review = review;
        this.imageUrl = imageUrl;
        this.displayOrder = displayOrder;
    }
}
