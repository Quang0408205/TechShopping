package com.example.Tech.entity.voucher;

import com.example.Tech.entity.product.Category;
import com.example.Tech.entity.user.User;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.LinkedHashSet;
import java.util.Set;

/**
 * An order voucher — discountPercent off the in-scope goods (after product promotions, shipping
 * excluded), capped by maxDiscount (null = no cap), when those goods reach minOrderAmount. Valid while
 * {@code active && startsAt <= now < endsAt}. usedCount counts orders not cancelled; it is changed under a row lock.
 */
@Entity
@Table(name = "vouchers")
@Getter
@Setter
@NoArgsConstructor
public class Voucher {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "voucher_id")
    private Long id;

    /** Upper case, 3–30 of A-Z 0-9 _ - (DB CHECK). */
    @Column(name = "code", length = 30, nullable = false, unique = true)
    private String code;

    @Column(name = "name", length = 150, nullable = false)
    private String name;

    @Column(name = "description", columnDefinition = "text")
    private String description;

    @Column(name = "discount_percent", precision = 5, scale = 2, nullable = false)
    private BigDecimal discountPercent;

    @Column(name = "max_discount", precision = 15, scale = 2)
    private BigDecimal maxDiscount;

    @Column(name = "min_order_amount", precision = 15, scale = 2, nullable = false)
    private BigDecimal minOrderAmount = BigDecimal.ZERO;

    @Column(name = "starts_at", nullable = false)
    private LocalDateTime startsAt;

    @Column(name = "ends_at", nullable = false)
    private LocalDateTime endsAt;

    /** null = unlimited. */
    @Column(name = "usage_limit")
    private Integer usageLimit;

    @Column(name = "per_customer_limit", nullable = false)
    private Integer perCustomerLimit = 1;

    @Column(name = "used_count", nullable = false)
    private Integer usedCount = 0;

    /** true = every category; false = only {@link #categories}. */
    @Column(name = "all_categories", nullable = false)
    private Boolean allCategories = true;

    @Column(name = "is_active", nullable = false)
    private Boolean active = true;

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(name = "voucher_categories",
            joinColumns = @JoinColumn(name = "voucher_id"),
            inverseJoinColumns = @JoinColumn(name = "category_id"))
    private Set<Category> categories = new LinkedHashSet<>();

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "created_by", nullable = false)
    private User createdBy;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    public boolean runningAt(LocalDateTime now) {
        return Boolean.TRUE.equals(active) && !now.isBefore(startsAt) && now.isBefore(endsAt);
    }
}
