package com.example.Tech.entity.order;

import com.example.Tech.entity.product.ProductVariant;
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

import java.math.BigDecimal;

/**
 * One line of an order, priced at checkout. variant_id has no ON DELETE action, so a variant that appears in
 * an order cannot be hard-deleted (ProductVariantService answers 409 RESOURCE_IN_USE). The product name and
 * image are read from the catalogue when the order is shown (no snapshot columns, decision 2026-10-02).
 */
@Entity
@Table(name = "order_items")
@Getter
@Setter
@NoArgsConstructor
public class OrderItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "order_item_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "order_id", nullable = false)
    private Order order;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "variant_id", nullable = false)
    private ProductVariant variant;

    /** DB CHECK quantity > 0. */
    @Column(name = "quantity", nullable = false)
    private Integer quantity;

    /** Price actually charged per unit (the variant's discount price when it was lower). */
    @Column(name = "unit_price", precision = 15, scale = 2, nullable = false)
    private BigDecimal unitPrice;

    /** (list price − unit price) × quantity; 0 without a discount. */
    @Column(name = "discount_amount", precision = 15, scale = 2)
    private BigDecimal discountAmount = BigDecimal.ZERO;

    /** unit price × quantity. */
    @Column(name = "subtotal", precision = 15, scale = 2, nullable = false)
    private BigDecimal subtotal;

    /** Plan v5: share of the order's voucher discount on this line (refunds pay back subtotal − this share). */
    @Column(name = "voucher_discount", precision = 15, scale = 2, nullable = false)
    private BigDecimal voucherDiscount = BigDecimal.ZERO;

    /** Plan v5 GĐ10: combo discount on this line (add-on units bought together with a main product). */
    @Column(name = "combo_discount", precision = 15, scale = 2, nullable = false)
    private BigDecimal comboDiscount = BigDecimal.ZERO;

    /** Plan v5 GĐ6: share of the order's loyalty-points discount on this line. */
    @Column(name = "points_discount", precision = 15, scale = 2, nullable = false)
    private BigDecimal pointsDiscount = BigDecimal.ZERO;

    /** The line total after the combo discount: what the voucher and the points are worked out on. */
    public BigDecimal afterCombo() {
        return subtotal.subtract(comboDiscount == null ? BigDecimal.ZERO : comboDiscount);
    }

    /** What the customer really paid in money for {@code units} of this line (combo + voucher + points shares split per unit). */
    public BigDecimal paidFor(int units) {
        BigDecimal gross = unitPrice.multiply(BigDecimal.valueOf(units));
        return gross.subtract(shareOf(comboDiscount, units)).subtract(shareOf(voucherDiscount, units))
                .subtract(shareOf(pointsDiscount, units));
    }

    /** The points-discount share of {@code units} of this line, in đồng. */
    public BigDecimal pointsShareFor(int units) {
        return shareOf(pointsDiscount, units);
    }

    private BigDecimal shareOf(BigDecimal lineDiscount, int units) {
        if (lineDiscount == null || lineDiscount.signum() == 0 || quantity == null || quantity == 0) {
            return BigDecimal.ZERO;
        }
        return lineDiscount.multiply(BigDecimal.valueOf(units))
                .divide(BigDecimal.valueOf(quantity), 0, java.math.RoundingMode.HALF_UP);
    }
}
