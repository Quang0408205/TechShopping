package com.example.Tech.dto.response.common;

import java.util.Map;

public record ApiError(
        String code,
        String message,
        Map<String, String> details
) {
}
