package com.example.Tech.security;

import com.example.Tech.service.audit.AuditLogService;
import com.example.Tech.service.audit.SensitiveData;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.util.ContentCachingRequestWrapper;
import tools.jackson.databind.json.JsonMapper;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Set;

/**
 * Writes one audit line for every write request (POST / PUT / PATCH / DELETE) that a staff account made and the API
 * accepted (2xx). Reading the JSON body needs a caching wrapper; the secrets in it are masked before it is stored.
 * Runs after the bearer token was read, so it sees who is calling; the line is written once the request has finished,
 * so a refused or failed request leaves nothing behind.
 */
@RequiredArgsConstructor
public class AuditLogFilter extends OncePerRequestFilter {

    private static final int BODY_CACHE_LIMIT = 16 * 1024;

    private static final Set<String> WRITE_METHODS = Set.of("POST", "PUT", "PATCH", "DELETE");

    private static final List<String> SKIPPED_PREFIXES = List.of("/api/v1/auth/", "/api/v1/chat", "/api/v1/recommendations",
            "/api/v1/interactions", "/api/v1/admin/notifications");

    private static final List<String> STAFF_ROLES = List.of("ADMIN", "BRANCH_MANAGER", "STAFF");

    private final AuditLogService auditLogService;
    private final JsonMapper jsonMapper;

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getRequestURI();
        return !WRITE_METHODS.contains(request.getMethod()) || !path.startsWith("/api/v1/")
                || SKIPPED_PREFIXES.stream().anyMatch(path::startsWith);
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String role = authentication instanceof JwtAuthenticationToken ? staffRole(authentication) : null;
        if (role == null) {
            chain.doFilter(request, response);
            return;
        }
        boolean json = request.getContentType() != null
                && MediaType.APPLICATION_JSON.isCompatibleWith(MediaType.parseMediaType(request.getContentType()));
        HttpServletRequest wrapped = json ? new ContentCachingRequestWrapper(request, BODY_CACHE_LIMIT) : request;
        chain.doFilter(wrapped, response);
        if (response.getStatus() < 200 || response.getStatus() >= 300) {
            return;
        }
        JwtAuthenticationToken jwt = (JwtAuthenticationToken) authentication;
        String body = wrapped instanceof ContentCachingRequestWrapper cached
                ? new String(cached.getContentAsByteArray(), StandardCharsets.UTF_8) : null;
        auditLogService.record(new AuditLogService.Entry(Long.valueOf(jwt.getToken().getSubject()),
                jwt.getToken().getClaimAsString(JwtConfig.USERNAME_CLAIM), null, role, request.getMethod(),
                request.getRequestURI(), response.getStatus(), clientIp(request), SensitiveData.mask(jsonMapper, body)));
    }

    /** The highest staff role of the token, or null when the caller is no staff member. */
    private static String staffRole(Authentication authentication) {
        List<String> roles = authentication.getAuthorities().stream().map(GrantedAuthority::getAuthority)
                .map(authority -> authority.startsWith("ROLE_") ? authority.substring(5) : authority).toList();
        return STAFF_ROLES.stream().filter(roles::contains).findFirst().orElse(null);
    }

    /** Behind the deploy proxy the TCP peer is the proxy, so the first X-Forwarded-For hop is what an auditor needs. */
    private static String clientIp(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        return forwarded != null && !forwarded.isBlank() ? forwarded.split(",")[0].trim() : request.getRemoteAddr();
    }
}
