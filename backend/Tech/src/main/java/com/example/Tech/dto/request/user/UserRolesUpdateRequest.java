package com.example.Tech.dto.request.user;

import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;

import java.util.List;

/**
 * The new role of an internal account: exactly one of STAFF / BRANCH_MANAGER (the list form is kept for the
 * existing API). CUSTOMER and ADMIN are never assigned through the API.
 */
public record UserRolesUpdateRequest(

        @ArraySchema(schema = @Schema(example = "BRANCH_MANAGER"))
        @NotEmpty(message = "At least one role is required")
        List<@NotBlank(message = "Role name must not be blank") String> roles
) {
}
