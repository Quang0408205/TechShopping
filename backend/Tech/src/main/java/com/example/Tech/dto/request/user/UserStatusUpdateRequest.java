package com.example.Tech.dto.request.user;

import jakarta.validation.constraints.NotNull;

public record UserStatusUpdateRequest(

        @NotNull(message = "Active flag is required")
        Boolean active
) {
}
