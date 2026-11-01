package com.example.Tech.dto.response.content;

import java.time.LocalDateTime;

public record BannerResponse(
        Integer id,
        String title,
        String subtitle,
        String imageUrl,
        String linkUrl,
        int displayOrder,
        boolean active,
        LocalDateTime startsAt,
        LocalDateTime endsAt
) {
}
