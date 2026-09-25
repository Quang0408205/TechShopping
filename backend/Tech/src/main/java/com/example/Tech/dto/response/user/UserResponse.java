package com.example.Tech.dto.response.user;

import java.time.LocalDateTime;
import java.util.List;

public record UserResponse(
        Long id,
        String email,
        String username,
        String fullname,
        String phone,
        String avatarUrl,
        Boolean isActive,
        List<String> roles,
        LocalDateTime lastLogin,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}
