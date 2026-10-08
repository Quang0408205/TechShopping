package com.example.Tech.entity.aftersales;

import com.example.Tech.entity.order.OrderItem;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDate;
import java.time.LocalDateTime;

/** Warranty of one sold order line, issued when the order is delivered (WarrantyIssuer). */
@Entity
@Table(name = "warranties")
@Getter
@Setter
@NoArgsConstructor
public class Warranty {

    public static final String TYPE_STANDARD = "STANDARD";

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "warranty_id")
    private Long id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "order_item_id", nullable = false, unique = true)
    private OrderItem orderItem;

    @Column(name = "warranty_start_date", nullable = false)
    private LocalDate startDate;

    @Column(name = "warranty_end_date", nullable = false)
    private LocalDate endDate;

    @Column(name = "warranty_type", length = 50)
    private String warrantyType = TYPE_STANDARD;

    @Column(name = "is_active")
    private Boolean active = true;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    public boolean isValidOn(LocalDate day) {
        return Boolean.TRUE.equals(active) && !day.isBefore(startDate) && !day.isAfter(endDate);
    }
}
