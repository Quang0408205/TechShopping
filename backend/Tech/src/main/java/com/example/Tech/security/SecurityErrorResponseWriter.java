package com.example.Tech.security;

import com.example.Tech.dto.response.common.ApiError;
import com.example.Tech.dto.response.common.ApiResult;
import com.example.Tech.exception.ErrorCode;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import tools.jackson.databind.json.JsonMapper;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

/**
 * Writes security errors (401/403 raised by filters, before any controller runs) in the same
 * ApiResult format as GlobalExceptionHandler.
 */
@Component
@RequiredArgsConstructor
public class SecurityErrorResponseWriter {

    private final JsonMapper jsonMapper;

    public void write(HttpServletResponse response, ErrorCode code) throws IOException {
        ApiResult<Void> body = ApiResult.fail(new ApiError(code.name(), code.getDefaultMessage(), null));
        response.setStatus(code.getStatus().value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        jsonMapper.writeValue(response.getOutputStream(), body);
    }
}
