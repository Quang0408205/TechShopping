package com.example.Tech.entity.wishlist;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * A product a customer saved (plan v7 GĐ4). priceAtAdd = the price when saved; lastAlertPrice = the price the next
 * price-drop mail is compared with (lowered after each mail, raised when the price goes up again).
 */
@Entity
@Table(name = "wishlist_items")
@Getter
@Setter
@NoArgsConstructor
public class WishlistItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "wishlist_item_id")
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "product_id", nullable = false)
    private Long productId;

    @Column(name = "price_at_add", precision = 15, scale = 2)
    private BigDecimal priceAtAdd;

    @Column(name = "last_alert_price", precision = 15, scale = 2)
    private BigDecimal lastAlertPrice;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    public WishlistItem(Long userId, Long productId, BigDecimal price) {
        this.userId = userId;
        this.productId = productId;
        this.priceAtAdd = price;
        this.lastAlertPrice = price;
    }
}
