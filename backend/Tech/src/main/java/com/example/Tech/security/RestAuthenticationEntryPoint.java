package com.example.Tech.security;

import com.example.Tech.exception.ErrorCode;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.server.resource.web.BearerTokenAuthenticationEntryPoint;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * 401 response: INVALID_TOKEN when a bearer token was sent but rejected (bad signature, expired,
 * wrong issuer), UNAUTHORIZED when no token was sent. The standard WWW-Authenticate header is kept.
 */
@Component
@RequiredArgsConstructor
public class RestAuthenticationEntryPoint implements AuthenticationEntryPoint {

    private final BearerTokenAuthenticationEntryPoint bearerEntryPoint = new BearerTokenAuthenticationEntryPoint();
    private final SecurityErrorResponseWriter errorWriter;

    @Override
    public void commence(HttpServletRequest request, HttpServletResponse response,
                         AuthenticationException authException) throws IOException {
        bearerEntryPoint.commence(request, response, authException);
        ErrorCode code = authException instanceof OAuth2AuthenticationException
                ? ErrorCode.INVALID_TOKEN
                : ErrorCode.UNAUTHORIZED;
        errorWriter.write(response, code);
    }
}
