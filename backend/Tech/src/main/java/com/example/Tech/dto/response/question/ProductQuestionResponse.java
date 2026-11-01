package com.example.Tech.dto.response.question;

import java.time.LocalDateTime;

/** A question as visitors see it: only answered, visible ones; the asker is shown by given name only. */
public record ProductQuestionResponse(
        Long id,
        String question,
        String answer,
        String askerName,
        LocalDateTime createdAt,
        LocalDateTime answeredAt
) {
}
