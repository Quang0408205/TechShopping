package com.example.Tech.entity.aftersales;

import com.example.Tech.entity.order.OrderItem;
import com.example.Tech.entity.user.User;
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
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/** Paid service on any bought item (no warranty needed); costs are recorded only, paid at the counter. */
@Entity
@Table(name = "maintenance_requests")
@Getter
@Setter
@NoArgsConstructor
public class MaintenanceRequest {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "maintenance_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "order_item_id", nullable = false)
    private OrderItem orderItem;

    @Enumerated(EnumType.STRING)
    @Column(name = "maintenance_type", length = 100)
    private MaintenanceType maintenanceType;

    @Column(name = "description", columnDefinition = "text", nullable = false)
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", length = 50, nullable = false)
    private ServiceRequestStatus status = ServiceRequestStatus.PENDING;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "assigned_to_employee")
    private User assignedTo;

    @Column(name = "estimated_completion_date")
    private LocalDate estimatedCompletionDate;

    @Column(name = "estimated_cost", precision = 15, scale = 2)
    private BigDecimal estimatedCost;

    @Column(name = "actual_cost", precision = 15, scale = 2)
    private BigDecimal actualCost;

    @Column(name = "received_at")
    private LocalDateTime receivedAt;

    /** The legacy column is a date: the day the request was completed. */
    @Column(name = "completion_date")
    private LocalDate completionDate;

    @Column(name = "rejection_reason", columnDefinition = "text")
    private String rejectionReason;

    @Column(name = "cancelled_at")
    private LocalDateTime cancelledAt;

    @Column(name = "notes", columnDefinition = "text")
    private String notes;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @OneToMany(mappedBy = "maintenanceRequest", cascade = CascadeType.PERSIST)
    @OrderBy("displayOrder")
    private List<ServiceRequestImage> images = new ArrayList<>();

    public void addImage(String imageUrl) {
        ServiceRequestImage image = new ServiceRequestImage(imageUrl, images.size() + 1);
        image.setMaintenanceRequest(this);
        images.add(image);
    }
}
