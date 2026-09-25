package com.example.Tech.controller.auth;

import com.example.Tech.entity.user.User;
import com.example.Tech.repository.user.CustomerProfileRepository;
import com.example.Tech.repository.user.UserRepository;
import com.example.Tech.repository.user.UserRoleRepository;
import com.example.Tech.security.RefreshTokenService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * End-to-end auth flow over HTTP against the test database (rolled back by @Transactional)
 * and the real Redis (every refresh token created here is revoked afterwards).
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class AuthApiIntegrationTest {

    private static final String PASSWORD = "Matkhau@123";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JsonMapper jsonMapper;

    @Autowired
    private JwtDecoder jwtDecoder;

    @Autowired
    private RefreshTokenService refreshTokenService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private UserRoleRepository userRoleRepository;

    @Autowired
    private CustomerProfileRepository customerProfileRepository;

    private final List<String> issuedRefreshTokens = new ArrayList<>();

    @AfterEach
    void revokeRefreshTokens() {
        issuedRefreshTokens.forEach(refreshTokenService::revoke);
    }

    private ResultActions postJson(String path, Object body) throws Exception {
        return mockMvc.perform(post(path)
                .contentType(MediaType.APPLICATION_JSON)
                .content(jsonMapper.writeValueAsString(body)));
    }

    private JsonNode data(ResultActions result) throws Exception {
        JsonNode data = jsonMapper.readTree(result.andReturn().getResponse().getContentAsString()).get("data");
        if (data != null && data.has("refreshToken")) {
            issuedRefreshTokens.add(data.get("refreshToken").asString());
        }
        return data;
    }

    private Map<String, Object> registerBody(String email, String username) {
        return Map.of("email", email, "username", username, "password", PASSWORD,
                "fullname", "Nguyễn Văn Test", "phone", "0901234567");
    }

    @Test
    void fullFlow_register_login_refresh_logout() throws Exception {
        // register → 201, user + CUSTOMER role + profile in the database
        JsonNode registered = data(postJson("/api/v1/auth/register", registerBody("Flow.Test@Example.com", "Flow.Test"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.tokenType").value("Bearer"))
                .andExpect(jsonPath("$.data.expiresIn").value(1800))
                .andExpect(jsonPath("$.data.user.email").value("flow.test@example.com"))
                .andExpect(jsonPath("$.data.user.username").value("flow.test"))
                .andExpect(jsonPath("$.data.user.roles[0]").value("CUSTOMER")));

        User user = userRepository.findByUsername("flow.test").orElseThrow();
        assertThat(user.getPasswordHash()).startsWith("$2a$").doesNotContain(PASSWORD);
        assertThat(userRoleRepository.findAllByIdUserId(user.getId()))
                .extracting(userRole -> userRole.getRole().getName()).containsExactly("CUSTOMER");
        assertThat(customerProfileRepository.findById(user.getId())).isPresent();

        Jwt jwt = jwtDecoder.decode(registered.get("accessToken").asString());
        assertThat(jwt.getSubject()).isEqualTo(String.valueOf(user.getId()));
        assertThat(jwt.getClaimAsStringList("roles")).containsExactly("CUSTOMER");

        // login with the username in another case → 200, last_login set
        JsonNode loggedIn = data(postJson("/api/v1/auth/login", Map.of("identifier", "FLOW.TEST", "password", PASSWORD))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.user.id").value(user.getId())));
        assertThat(userRepository.findById(user.getId()).orElseThrow().getLastLogin()).isNotNull();

        // the access token is accepted by the API
        mockMvc.perform(get("/api/v1/products").param("size", "1")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + loggedIn.get("accessToken").asString()))
                .andExpect(status().isOk());

        // refresh rotates: the new token works once, the old one is rejected
        String oldRefresh = loggedIn.get("refreshToken").asString();
        JsonNode refreshed = data(postJson("/api/v1/auth/refresh", Map.of("refreshToken", oldRefresh))
                .andExpect(status().isOk()));
        String newRefresh = refreshed.get("refreshToken").asString();
        assertThat(newRefresh).isNotEqualTo(oldRefresh);

        postJson("/api/v1/auth/refresh", Map.of("refreshToken", oldRefresh))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error.code").value("INVALID_TOKEN"));

        // logout revokes the current refresh token
        postJson("/api/v1/auth/logout", Map.of("refreshToken", newRefresh))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
        postJson("/api/v1/auth/refresh", Map.of("refreshToken", newRefresh))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error.code").value("INVALID_TOKEN"));
    }

    @Test
    void login_byEmail_andWrongPassword() throws Exception {
        data(postJson("/api/v1/auth/register", registerBody("login.test@example.com", "login.test"))
                .andExpect(status().isCreated()));

        data(postJson("/api/v1/auth/login", Map.of("identifier", "Login.Test@Example.com", "password", PASSWORD))
                .andExpect(status().isOk()));

        postJson("/api/v1/auth/login", Map.of("identifier", "login.test", "password", "wrong-password"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error.code").value("INVALID_CREDENTIALS"));
        postJson("/api/v1/auth/login", Map.of("identifier", "nobody.here", "password", PASSWORD))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error.code").value("INVALID_CREDENTIALS"));
    }

    @Test
    void register_duplicateEmailOrUsername_returns409() throws Exception {
        data(postJson("/api/v1/auth/register", registerBody("dup.test@example.com", "dup.test"))
                .andExpect(status().isCreated()));

        postJson("/api/v1/auth/register", registerBody("DUP.TEST@example.com", "other.name"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error.code").value("DUPLICATE_EMAIL"));
        postJson("/api/v1/auth/register", registerBody("other@example.com", "Dup.Test"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error.code").value("DUPLICATE_USERNAME"));
    }

    @Test
    void register_invalidData_returns400WithFieldDetails() throws Exception {
        postJson("/api/v1/auth/register", Map.of("email", "not-an-email", "username", "a b",
                "password", "short", "fullname", ""))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.error.details.email").exists())
                .andExpect(jsonPath("$.error.details.username").exists())
                .andExpect(jsonPath("$.error.details.password").exists())
                .andExpect(jsonPath("$.error.details.fullname").exists());
    }

    @Test
    void refreshAndLogout_requireAToken() throws Exception {
        postJson("/api/v1/auth/refresh", Map.of("refreshToken", ""))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("VALIDATION_ERROR"));
        postJson("/api/v1/auth/refresh", Map.of("refreshToken", "unknown-token"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error.code").value("INVALID_TOKEN"));
        postJson("/api/v1/auth/logout", Map.of("refreshToken", "unknown-token"))
                .andExpect(status().isOk());
    }
}
