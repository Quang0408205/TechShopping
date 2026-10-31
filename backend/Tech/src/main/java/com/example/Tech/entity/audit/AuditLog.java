package com.example.Tech.entity.audit;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

/**
 * One successful write request (POST / PUT / PATCH / DELETE) of a staff account. Append-only: nothing in the
 * application updates or deletes a row. The user is kept by value (no foreign key) so the trail outlives the account.
 */
@Entity
@Table(name = "audit_logs")
@Getter
@Setter
@NoArgsConstructor
public class AuditLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "log_id")
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "username", length = 50, nullable = false)
    private String username;

    @Column(name = "full_name", length = 120)
    private String fullName;

    @Column(name = "role", length = 20, nullable = false)
    private String role;

    @Column(name = "method", length = 8, nullable = false)
    private String method;

    @Column(name = "path", length = 300, nullable = false)
    private String path;

    @Column(name = "action_label", length = 150, nullable = false)
    private String actionLabel;

    @Column(name = "entity_type", length = 50, nullable = false)
    private String entityType;

    @Column(name = "entity_id", length = 40)
    private String entityId;

    @Column(name = "status", nullable = false)
    private int status;

    @Column(name = "ip", length = 64)
    private String ip;

    @Column(name = "request_data", columnDefinition = "text")
    private String requestData;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;
}
