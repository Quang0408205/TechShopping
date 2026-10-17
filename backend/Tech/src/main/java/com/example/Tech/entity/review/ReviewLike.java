package com.example.Tech.entity.review;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.io.Serializable;
import java.time.LocalDateTime;

/** One account's "Hữu ích" on one review (primary key = both ids, so a second like is impossible). */
@Entity
@Table(name = "review_likes")
@Getter
@NoArgsConstructor
public class ReviewLike {

    @EmbeddedId
    private Key id;

    @Column(name = "created_at")
    private LocalDateTime createdAt = LocalDateTime.now();

    public ReviewLike(Long reviewId, Long userId) {
        this.id = new Key(reviewId, userId);
    }

    @Embeddable
    @Getter
    @Setter
    @NoArgsConstructor
    @EqualsAndHashCode
    public static class Key implements Serializable {

        @Column(name = "review_id")
        private Long reviewId;

        @Column(name = "user_id")
        private Long userId;

        public Key(Long reviewId, Long userId) {
            this.reviewId = reviewId;
            this.userId = userId;
        }
    }
}
