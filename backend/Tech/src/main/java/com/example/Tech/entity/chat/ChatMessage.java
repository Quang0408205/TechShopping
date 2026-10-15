package com.example.Tech.entity.chat;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "chat_messages")
@Getter
@NoArgsConstructor
public class ChatMessage {

    public static final String SENDER_USER = "USER";
    public static final String SENDER_BOT = "BOT";

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "message_id")
    private Long id;

    @Column(name = "session_id", nullable = false)
    private Long sessionId;

    @Column(name = "sender", nullable = false, length = 50)
    private String sender;

    @Column(name = "content", nullable = false)
    private String content;

    @Column(name = "message_type", length = 50)
    private String messageType = "TEXT";

    // "timestamp" is a SQL keyword, so the column name is quoted
    @Column(name = "`timestamp`")
    private LocalDateTime sentAt;

    public ChatMessage(Long sessionId, String sender, String content, LocalDateTime sentAt) {
        this.sessionId = sessionId;
        this.sender = sender;
        this.content = content;
        this.sentAt = sentAt;
    }

    public boolean fromUser() {
        return SENDER_USER.equals(sender);
    }
}
