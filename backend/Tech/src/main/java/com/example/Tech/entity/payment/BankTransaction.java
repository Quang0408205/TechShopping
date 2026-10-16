package com.example.Tech.entity.payment;

import com.example.Tech.entity.order.Order;
import com.example.Tech.entity.user.User;
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
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Money that arrived on the shop bank account, as reported by the bank webhook. {@code externalId} is unique, so a
 * webhook sent twice is stored once.
 */
@Entity
@Table(name = "bank_transactions")
@Getter
@Setter
@NoArgsConstructor
public class BankTransaction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "bank_transaction_id")
    private Long id;

    @Column(name = "external_id", length = 100, nullable = false, updatable = false)
    private String externalId;

    @Column(name = "provider", length = 30, nullable = false)
    private String provider = "WEBHOOK";

    @Column(name = "amount", precision = 15, scale = 2, nullable = false)
    private BigDecimal amount;

    @Column(name = "content", length = 500)
    private String content;

    @Column(name = "account_number", length = 30)
    private String accountNumber;

    @Column(name = "transferred_at")
    private LocalDateTime transferredAt;

    @Column(name = "received_at", nullable = false)
    private LocalDateTime receivedAt;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", length = 20, nullable = false)
    private BankTransactionStatus status;

    @Column(name = "reason", length = 255)
    private String reason;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id")
    private Order order;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "resolved_by")
    private User resolvedBy;

    @Column(name = "resolved_at")
    private LocalDateTime resolvedAt;

    @Column(name = "note", length = 500)
    private String note;
}
