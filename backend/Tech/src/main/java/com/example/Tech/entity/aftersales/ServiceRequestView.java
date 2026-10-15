package com.example.Tech.entity.aftersales;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.Immutable;

import java.time.LocalDateTime;

/** Read-only row of service_requests_view: the three request tables in one list (filter + page in the DB). */
@Entity
@Immutable
@Table(name = "service_requests_view")
@Getter
@NoArgsConstructor
public class ServiceRequestView {

    /** "WARRANTY-12", "MAINTENANCE-3", "RETURN-7". */
    @Id
    @Column(name = "view_id")
    private String viewId;

    @Enumerated(EnumType.STRING)
    @Column(name = "request_type")
    private ServiceRequestType requestType;

    @Column(name = "request_id")
    private Long requestId;

    @Column(name = "user_id")
    private Long userId;

    @Column(name = "order_id")
    private Long orderId;

    @Column(name = "store_id")
    private Integer storeId;

    @Column(name = "order_item_id")
    private Long orderItemId;

    /** Text: the two status enums share most values but not all. */
    @Column(name = "status")
    private String status;

    @Column(name = "description", columnDefinition = "text")
    private String description;

    @Column(name = "assigned_to_employee")
    private Long assignedToEmployee;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    public static String viewId(ServiceRequestType type, Long id) {
        return type.name() + "-" + id;
    }
}
