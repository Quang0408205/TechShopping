package com.example.Tech.exception;

import com.example.Tech.dto.response.common.ApiResult;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.InsufficientAuthenticationException;
import org.springframework.security.authorization.AuthorizationDecision;
import org.springframework.security.authorization.AuthorizationDeniedException;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Security exceptions thrown inside controllers (method security) must not fall into the 500 catch-all.
 */
class GlobalExceptionHandlerSecurityTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    @Test
    void authorizationDenied_isMappedTo403() {
        ResponseEntity<ApiResult<Void>> response = handler.handleAccessDenied(
                new AuthorizationDeniedException("Access Denied", new AuthorizationDecision(false)));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(response.getBody().error().code()).isEqualTo("ACCESS_DENIED");
    }

    @Test
    void authenticationException_isMappedTo401() {
        ResponseEntity<ApiResult<Void>> response = handler.handleAuthentication(
                new InsufficientAuthenticationException("Full authentication is required"));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(response.getBody().error().code()).isEqualTo("UNAUTHORIZED");
    }
}
