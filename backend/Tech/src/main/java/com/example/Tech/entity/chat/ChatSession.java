package com.example.Tech.entity.chat;

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

/**
 * One chatbot conversation. user_id is null for a guest. The browser keeps the random session token, never
 * the sequential id, so a conversation cannot be guessed.
 */
@Entity
@Table(name = "chat_sessions")
@Getter
@Setter
@NoArgsConstructor
public class ChatSession {

    public static final String STATUS_OPEN = "OPEN";

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "session_id")
    private Long id;

    @Column(name = "user_id")
    private Long userId;

    @Column(name = "session_token", length = 64, unique = true)
    private String token;

    @Column(name = "topic")
    private String topic;

    @Column(name = "status", length = 50)
    private String status = STATUS_OPEN;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "last_message_at")
    private LocalDateTime lastMessageAt;

    public ChatSession(Long userId, String token, String topic, LocalDateTime now) {
        this.userId = userId;
        this.token = token;
        this.topic = topic;
        this.createdAt = now;
        this.lastMessageAt = now;
    }
}
