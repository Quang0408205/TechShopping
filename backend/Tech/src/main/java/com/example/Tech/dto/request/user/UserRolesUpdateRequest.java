package com.example.Tech.dto.request.user;

import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;

import java.util.List;

/**
 * The complete new role set of a user (replaces the current roles).
 */
public record UserRolesUpdateRequest(

        @ArraySchema(schema = @Schema(example = "STAFF"))
        @NotEmpty(message = "At least one role is required")
        List<@NotBlank(message = "Role name must not be blank") String> roles
) {
}
