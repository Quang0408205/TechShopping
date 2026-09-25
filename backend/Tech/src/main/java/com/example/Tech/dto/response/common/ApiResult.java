package com.example.Tech.dto.response.common;

import java.time.LocalDateTime;

public record ApiResult<T>(
        boolean success,
        LocalDateTime timestamp,
        T data,
        ApiError error
) {

    public static <T> ApiResult<T> ok(T data) {
        return new ApiResult<>(true, LocalDateTime.now(), data, null);
    }

    public static <T> ApiResult<T> fail(ApiError error) {
        return new ApiResult<>(false, LocalDateTime.now(), null, error);
    }
}
