package com.example.Tech.dto.response.chat;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;

public record AdminChatSessionResponse(
        Long id,

        @Schema(description = "Null for a guest conversation")
        Long userId,

        @Schema(description = "Customer name, or \"Khách chưa đăng nhập\"")
        String customerName,

        String customerEmail,

        @Schema(description = "First question of the conversation")
        String topic,

        long messageCount,

        LocalDateTime createdAt,

        LocalDateTime lastMessageAt
) {
}
