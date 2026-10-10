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

        @Schema(description = "null / empty = no avatar; otherwise a URL returned by POST /api/v1/uploads/avatar "
                + "(links to other sites are refused)", example = "http://localhost:8080/uploads/avatars/3f2c….png")
        @Size(max = 2048, message = "Avatar URL must be at most 2048 characters")
        String avatarUrl
) {
}
