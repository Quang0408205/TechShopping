package com.example.Tech.entity.recommendation;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * One choice of a customer in the shopping-preference questionnaire (recommendations for a customer without any
 * activity yet). Exactly one of category, brand, need or price level is set (checked by the database).
 */
@Entity
@Table(name = "user_preferences")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class UserPreference {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "user_preference_id")
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "category_id")
    private Integer categoryId;

    @Column(name = "brand_id")
    private Integer brandId;

    @Column(name = "need_id")
    private Integer needId;

    @Enumerated(EnumType.STRING)
    @Column(name = "price_level", length = 10)
    private PriceLevel priceLevel;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    private UserPreference(Long userId, LocalDateTime createdAt) {
        this.userId = userId;
        this.createdAt = createdAt;
    }

    public static UserPreference category(Long userId, Integer categoryId, LocalDateTime at) {
        UserPreference preference = new UserPreference(userId, at);
        preference.categoryId = categoryId;
        return preference;
    }

    public static UserPreference brand(Long userId, Integer brandId, LocalDateTime at) {
        UserPreference preference = new UserPreference(userId, at);
        preference.brandId = brandId;
        return preference;
    }

    public static UserPreference need(Long userId, Integer needId, LocalDateTime at) {
        UserPreference preference = new UserPreference(userId, at);
        preference.needId = needId;
        return preference;
    }

    public static UserPreference price(Long userId, PriceLevel level, LocalDateTime at) {
        UserPreference preference = new UserPreference(userId, at);
        preference.priceLevel = level;
        return preference;
    }
}
