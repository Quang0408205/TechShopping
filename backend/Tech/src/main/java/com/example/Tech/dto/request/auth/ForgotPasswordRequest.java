package com.example.Tech.dto.request.auth;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ForgotPasswordRequest(

        @NotBlank(message = "Email is required")
        @Email(message = "Email is not valid")
        @Size(max = 120, message = "Email must be at most 120 characters")
        String email
) {
}
