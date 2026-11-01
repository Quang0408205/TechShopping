package com.example.Tech.dto.response.content;

import java.time.LocalDateTime;

/** {@code body} is null in a list (titles only). */
public record ContentPageResponse(
        String slug,
        String title,
        String body,
        boolean published,
        LocalDateTime updatedAt
) {
}
