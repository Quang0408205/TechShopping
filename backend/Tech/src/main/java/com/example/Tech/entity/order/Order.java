package com.example.Tech.entity.order;

import com.example.Tech.entity.combo.OrderCombo;
import com.example.Tech.entity.store.Store;
import com.example.Tech.entity.user.User;
import com.example.Tech.entity.voucher.Voucher;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.List;

/**
 * An order placed from the customer's server cart. Prices are copied into the order and its items
 * at checkout, so later catalogue price changes do not affect it. users are only soft-deleted, so user_id
 * (no ON DELETE action) always points to an existing row. store = the branch that handles it.
 */
@Entity
@Table(name = "orders")
@Getter
@Setter
@NoArgsConstructor
public class Order {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "order_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "order_date")
    private LocalDateTime orderDate;

    @Column(name = "recipient_name", length = 120, nullable = false)
    private String recipientName;

    @Column(name = "recipient_phone", length = 20, nullable = false)
    private String recipientPhone;

    @Column(name = "shipping_address", columnDefinition = "text", nullable = false)
    private String shippingAddress;

    /** Province / ward picked from the 2-level list (null for pickup and older orders). */
    @Column(name = "shipping_province", length = 100)
    private String shippingProvince;

    @Column(name = "shipping_ward", length = 100)
    private String shippingWard;

    @Column(name = "billing_address", columnDefinition = "text")
    private String billingAddress;

    @Column(name = "shipping_cost", precision = 15, scale = 2)
    private BigDecimal shippingCost = BigDecimal.ZERO;

    /** Prices include VAT, so this stays 0 (decision 2026-10-02). */
    @Column(name = "tax_amount", precision = 15, scale = 2)
    private BigDecimal taxAmount = BigDecimal.ZERO;

    /** Sum of the item subtotals − combo discount − voucher discount − points discount + shipping cost + tax. */
    @Column(name = "total_amount", precision = 15, scale = 2, nullable = false)
    private BigDecimal totalAmount;

    /** Total combo discount (the sum over the items); taken off before the voucher. */
    @Column(name = "combo_discount", precision = 15, scale = 2, nullable = false)
    private BigDecimal comboDiscount = BigDecimal.ZERO;

    /** The combos applied (name copied), for the order page and the combo report. */
    @OneToMany(mappedBy = "order", cascade = CascadeType.PERSIST)
    @OrderBy("id")
    private List<OrderCombo> combos = new ArrayList<>();

    /** Voucher used at checkout (code + amount copied, so later voucher edits do not change the order). */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "voucher_id")
    private Voucher voucher;

    @Column(name = "voucher_code", length = 30)
    private String voucherCode;

    @Column(name = "voucher_discount", precision = 15, scale = 2, nullable = false)
    private BigDecimal voucherDiscount = BigDecimal.ZERO;

    /** Loyalty points spent at checkout and the amount they took off (1 point = 1.000đ). */
    @Column(name = "points_used", nullable = false)
    private int pointsUsed;

    @Column(name = "points_discount", precision = 15, scale = 2, nullable = false)
    private BigDecimal pointsDiscount = BigDecimal.ZERO;

    /** Points credited when the order was delivered (multiplier of the customer's tier at that time). */
    @Column(name = "points_earned", nullable = false)
    private int pointsEarned;

    /** Where the order mails go (typed at checkout); null on older orders = the account's email. */
    @Column(name = "contact_email", length = 255)
    private String contactEmail;

    /** Random code of the public tracking link (customer/track.html?token=…); never shown with the id. */
    @Column(name = "tracking_token", length = 64, nullable = false, updatable = false)
    private String trackingToken = newTrackingToken();

    @Enumerated(EnumType.STRING)
    @Column(name = "status", length = 50)
    private OrderStatus status = OrderStatus.PENDING;

    @Enumerated(EnumType.STRING)
    @Column(name = "payment_method", length = 50)
    private PaymentMethod paymentMethod;

    @Column(name = "tracking_number", length = 100)
    private String trackingNumber;

    @Column(name = "notes", columnDefinition = "text")
    private String notes;

    @Column(name = "delivered_at")
    private LocalDateTime deliveredAt;

    @Column(name = "cancelled_at")
    private LocalDateTime cancelledAt;

    /** Store that handles the order and whose stock is used; null for orders placed before it. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "store_id")
    private Store store;

    @Enumerated(EnumType.STRING)
    @Column(name = "delivery_type", length = 20, nullable = false)
    private DeliveryType deliveryType = DeliveryType.HOME_DELIVERY;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    /** Saved together with the order (cascade PERSIST); never changed afterwards. */
    @OneToMany(mappedBy = "order", cascade = CascadeType.PERSIST)
    @OrderBy("id")
    private List<OrderItem> items = new ArrayList<>();

    private static String newTrackingToken() {
        byte[] bytes = new byte[32];
        new SecureRandom().nextBytes(bytes);
        return HexFormat.of().formatHex(bytes);
    }

    public void addCombo(OrderCombo combo) {
        combo.setOrder(this);
        combos.add(combo);
    }

    public void addItem(OrderItem item) {
        item.setOrder(this);
        items.add(item);
    }
}
