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

import java.time.LocalDateTime;

/**
 * One behaviour of a logged-in user on a product: the input of the recommendation models (AI_Service).
 * Ids instead of associations: rows are only inserted here and read in bulk by the AI service.
 */
@Entity
@Table(name = "user_interactions")
@Getter
@NoArgsConstructor
public class UserInteraction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "interaction_id")
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "product_id", nullable = false)
    private Long productId;

    @Enumerated(EnumType.STRING)
    @Column(name = "interaction_type", nullable = false, length = 50)
    private InteractionType type;

    @Column(name = "interaction_score")
    private Integer score;

    // "timestamp" is a SQL keyword, so the column name is quoted
    @Column(name = "`timestamp`")
    private LocalDateTime occurredAt;

    public UserInteraction(Long userId, Long productId, InteractionType type, LocalDateTime occurredAt) {
        this.userId = userId;
        this.productId = productId;
        this.type = type;
        this.score = type.weight();
        this.occurredAt = occurredAt;
    }
}
