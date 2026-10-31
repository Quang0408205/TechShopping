package com.example.Tech.dto.request.auth;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record GoogleLoginRequest(

        @NotBlank(message = "Google credential is required")
        @Size(max = 4096, message = "Google credential is too long")
        String credential
) {
}
