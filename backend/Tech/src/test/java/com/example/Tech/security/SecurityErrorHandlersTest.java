package com.example.Tech.security;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.InsufficientAuthenticationException;
import org.springframework.security.oauth2.server.resource.InvalidBearerTokenException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

import static org.assertj.core.api.Assertions.assertThat;

class SecurityErrorHandlersTest {

    private final JsonMapper jsonMapper = JsonMapper.builder().build();
    private final SecurityErrorResponseWriter writer = new SecurityErrorResponseWriter(jsonMapper);

    @Test
    void entryPoint_withoutToken_returns401Unauthorized() throws Exception {
        MockHttpServletResponse response = new MockHttpServletResponse();

        new RestAuthenticationEntryPoint(writer).commence(new MockHttpServletRequest(), response,
                new InsufficientAuthenticationException("Full authentication is required"));

        assertThat(response.getStatus()).isEqualTo(401);
        assertThat(response.getHeader(HttpHeaders.WWW_AUTHENTICATE)).startsWith("Bearer");
        JsonNode body = jsonMapper.readTree(response.getContentAsString());
        assertThat(body.get("success").asBoolean()).isFalse();
        assertThat(body.get("error").get("code").asString()).isEqualTo("UNAUTHORIZED");
        assertThat(body.get("timestamp").isNull()).isFalse();
    }

    @Test
    void entryPoint_withInvalidToken_returns401InvalidToken() throws Exception {
        MockHttpServletResponse response = new MockHttpServletResponse();

        new RestAuthenticationEntryPoint(writer).commence(new MockHttpServletRequest(), response,
                new InvalidBearerTokenException("Jwt expired"));

        assertThat(response.getStatus()).isEqualTo(401);
        assertThat(response.getHeader(HttpHeaders.WWW_AUTHENTICATE)).contains("invalid_token");
        assertThat(jsonMapper.readTree(response.getContentAsString()).get("error").get("code").asString())
                .isEqualTo("INVALID_TOKEN");
    }

    @Test
    void accessDeniedHandler_returns403AccessDenied() throws Exception {
        MockHttpServletResponse response = new MockHttpServletResponse();

        new RestAccessDeniedHandler(writer).handle(new MockHttpServletRequest(), response,
                new AccessDeniedException("Access Denied"));

        assertThat(response.getStatus()).isEqualTo(403);
        assertThat(response.getContentType()).startsWith("application/json");
        assertThat(jsonMapper.readTree(response.getContentAsString()).get("error").get("code").asString())
                .isEqualTo("ACCESS_DENIED");
    }
}
