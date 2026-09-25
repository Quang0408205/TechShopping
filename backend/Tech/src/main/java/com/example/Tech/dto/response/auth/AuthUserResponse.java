package com.example.Tech.dto.response.auth;

import java.util.List;

public record AuthUserResponse(
        Long id,
        String email,
        String username,
        String fullname,
        List<String> roles
) {
}
