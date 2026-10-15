package com.example.Tech.entity.recommendation;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/** One product shown to a logged-in user by the recommendation system, with its score and reason (log). */
@Entity
@Table(name = "recommendations")
@Getter
@NoArgsConstructor
public class Recommendation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "recommendation_id")
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "product_id", nullable = false)
    private Long productId;

    @Enumerated(EnumType.STRING)
    @Column(name = "recommendation_type", length = 50)
    private RecommendationType type;

    @Column(name = "score", precision = 5, scale = 3)
    private BigDecimal score;

    @Column(name = "reason")
    private String reason;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    public Recommendation(Long userId, Long productId, RecommendationType type, BigDecimal score, String reason,
                          LocalDateTime createdAt) {
        this.userId = userId;
        this.productId = productId;
        this.type = type;
        this.score = score;
        this.reason = reason;
        this.createdAt = createdAt;
    }
}
