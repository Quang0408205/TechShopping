package com.example.Tech.security;

import com.example.Tech.exception.ErrorCode;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpMethod;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * While the access token carries the {@code pwd_change} claim (temporary password not yet replaced) the account may only
 * read itself, change its password and use /auth/*; every other API answers 403 PASSWORD_CHANGE_REQUIRED.
 * Not a Spring bean: SecurityConfig adds it to the security chain only (a bean would also register it as a servlet filter).
 */
@RequiredArgsConstructor
public class PasswordChangeRequiredFilter extends OncePerRequestFilter {

    private static final String ME_PATH = "/api/v1/users/me";

    private final SecurityErrorResponseWriter errorWriter;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication instanceof JwtAuthenticationToken jwt
                && Boolean.TRUE.equals(jwt.getToken().getClaim(JwtConfig.PASSWORD_CHANGE_CLAIM))
                && !allowed(request)) {
            errorWriter.write(response, ErrorCode.PASSWORD_CHANGE_REQUIRED);
            return;
        }
        chain.doFilter(request, response);
    }

    private static boolean allowed(HttpServletRequest request) {
        String path = request.getRequestURI();
        String method = request.getMethod();
        return HttpMethod.OPTIONS.matches(method)
                || path.startsWith("/api/v1/auth/")
                || (HttpMethod.GET.matches(method) && path.equals(ME_PATH))
                || (HttpMethod.PUT.matches(method) && path.equals(ME_PATH + "/password"));
    }
}
