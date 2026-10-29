package com.example.Tech.entity.loyalty;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/** A membership tier (plan v5 GĐ6). The 4 codes are fixed; ADMIN edits the threshold and the point multiplier. */
@Entity
@Table(name = "loyalty_tiers")
@Getter
@Setter
@NoArgsConstructor
public class LoyaltyTier {

    public static final String MEMBER = "MEMBER";

    @Id
    @Column(name = "tier_code", length = 20)
    private String code;

    @Column(name = "name", length = 50, nullable = false)
    private String name;

    /** Total spent on delivered orders needed to reach this tier. */
    @Column(name = "min_spent", precision = 15, scale = 2, nullable = false)
    private BigDecimal minSpent;

    @Column(name = "point_multiplier", precision = 4, scale = 2, nullable = false)
    private BigDecimal pointMultiplier;

    @Column(name = "sort_order", nullable = false)
    private int sortOrder;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    public LoyaltyTier(String code, String name, BigDecimal minSpent, BigDecimal pointMultiplier, int sortOrder) {
        this.code = code;
        this.name = name;
        this.minSpent = minSpent;
        this.pointMultiplier = pointMultiplier;
        this.sortOrder = sortOrder;
    }
}
