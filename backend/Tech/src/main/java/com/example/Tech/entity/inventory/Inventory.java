package com.example.Tech.entity.inventory;

import com.example.Tech.entity.product.ProductVariant;
import com.example.Tech.entity.store.Store;
import jakarta.persistence.Column;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.MapsId;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

/**
 * Current stock quantity of one product variant at one store. Quantity-only (no IMEI/serial
 * tracking), no store-to-store transfer. Every change to {@code quantity} must also append a row to
 * {@link StockMovement} (stock-in, order confirmation, order-cancel restore) — never update this row
 * without a matching movement, so {@code stock_movements} stays a complete, auditable history.
 */
@Entity
@Table(name = "inventory")
@Getter
@Setter
@NoArgsConstructor
public class Inventory {

    @EmbeddedId
    private InventoryId id;

    @MapsId("storeId")
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "store_id", nullable = false)
    private Store store;

    @MapsId("variantId")
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "variant_id", nullable = false)
    private ProductVariant variant;

    @Column(name = "quantity", nullable = false)
    private Integer quantity = 0;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    public Inventory(Store store, ProductVariant variant, int quantity) {
        this.id = new InventoryId(store.getId(), variant.getId());
        this.store = store;
        this.variant = variant;
        this.quantity = quantity;
    }
}
