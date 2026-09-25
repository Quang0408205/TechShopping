package com.example.Tech.dto.request.user;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Own account data. Email and username cannot be changed. PUT semantics: omitted optional fields are cleared.
 */
public record UserUpdateRequest(

        @Schema(example = "Nguyễn Văn An")
        @NotBlank(message = "Full name is required")
        @Size(max = 120, message = "Full name must be at most 120 characters")
        String fullname,

        @Schema(example = "0901234567")
        @Size(max = 20, message = "Phone must be at most 20 characters")
        String phone,

        @Schema(example = "https://example.com/avatar.png")
        @Size(max = 2048, message = "Avatar URL must be at most 2048 characters")
        String avatarUrl
) {
}
