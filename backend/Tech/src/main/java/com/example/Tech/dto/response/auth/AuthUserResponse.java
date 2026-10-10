package com.example.Tech.dto.response.auth;

import java.util.List;

public record AuthUserResponse(
        Long id,
        String email,
        String username,
        String fullname,
        List<String> roles,
        /* kế hoạch v2 GĐ5: the header shows the avatar right after login / refresh */
        String avatarUrl
) {
}
