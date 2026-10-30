package com.example.Tech.entity.mail;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

/** One order mail that was sent, failed or skipped. order_id is cleared if the order is ever deleted. */
@Entity
@Table(name = "email_log")
@Getter
@Setter
@NoArgsConstructor
public class EmailLog {

    public static final String SENT = "SENT";
    public static final String FAILED = "FAILED";
    public static final String SKIPPED = "SKIPPED";

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "email_log_id")
    private Long id;

    @Column(name = "order_id")
    private Long orderId;

    @Column(name = "kind", length = 30, nullable = false)
    private String kind;

    @Column(name = "recipient", length = 255, nullable = false)
    private String recipient;

    @Column(name = "subject", length = 255, nullable = false)
    private String subject;

    @Column(name = "status", length = 10, nullable = false)
    private String status;

    @Column(name = "error", length = 500)
    private String error;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    public EmailLog(Long orderId, String kind, String recipient, String subject, String status, String error) {
        this.orderId = orderId;
        this.kind = kind;
        this.recipient = recipient;
        this.subject = subject;
        this.status = status;
        this.error = error == null ? null : error.substring(0, Math.min(error.length(), 500));
    }
}
