package com.example.Tech.controller.user;

import com.example.Tech.entity.user.User;
import com.example.Tech.repository.user.CustomerProfileRepository;
import com.example.Tech.repository.user.UserRepository;
import com.example.Tech.security.RefreshTokenService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * /api/v1/users/me over HTTP against the test database (rolled back) and the real Redis
 * (all refresh tokens of the test user are revoked afterwards).
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class UserApiIntegrationTest {

    private static final String PASSWORD = "Matkhau@123";
    private static final String NEW_PASSWORD = "MatkhauMoi@456";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JsonMapper jsonMapper;

    @Autowired
    private RefreshTokenService refreshTokenService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private CustomerProfileRepository customerProfileRepository;

    private Long userId;
    private String accessToken;
    private String refreshToken;

    @BeforeEach
    void registerUser() throws Exception {
        JsonNode data = data(send(post("/api/v1/auth/register"), null, Map.of(
                "email", "me.test@example.com", "username", "me.test", "password", PASSWORD,
                "fullname", "Nguyễn Văn Me", "phone", "0901234567"))
                .andExpect(status().isCreated()));
        userId = data.get("user").get("id").asLong();
        accessToken = data.get("accessToken").asString();
        refreshToken = data.get("refreshToken").asString();
    }

    @AfterEach
    void revokeTokens() {
        refreshTokenService.revokeAll(userId);
    }

    private ResultActions send(MockHttpServletRequestBuilder builder, String token, Object body) throws Exception {
        if (token != null) {
            builder.header(HttpHeaders.AUTHORIZATION, "Bearer " + token);
        }
        if (body != null) {
            builder.contentType(MediaType.APPLICATION_JSON).content(jsonMapper.writeValueAsString(body));
        }
        return mockMvc.perform(builder);
    }

    private JsonNode data(ResultActions result) throws Exception {
        return jsonMapper.readTree(result.andReturn().getResponse().getContentAsString()).get("data");
    }

    private static Map<String, Object> profileBody(String gender, String dateOfBirth) {
        Map<String, Object> body = new HashMap<>();
        body.put("dateOfBirth", dateOfBirth);
        body.put("gender", gender);
        body.put("address", "123 Nguyễn Huệ");
        body.put("city", "TP. Hồ Chí Minh");
        body.put("district", "Quận 1");
        body.put("ward", "Phường Bến Nghé");
        return body;
    }

    @Test
    void getMe_requiresToken_andReturnsOwnAccount() throws Exception {
        send(get("/api/v1/users/me"), null, null)
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error.code").value("UNAUTHORIZED"));

        send(get("/api/v1/users/me"), accessToken, null)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value(userId))
                .andExpect(jsonPath("$.data.email").value("me.test@example.com"))
                .andExpect(jsonPath("$.data.username").value("me.test"))
                .andExpect(jsonPath("$.data.isActive").value(true))
                .andExpect(jsonPath("$.data.roles[0]").value("CUSTOMER"))
                .andExpect(jsonPath("$.data.passwordHash").doesNotExist());
    }

    @Test
    void updateMe_changesProfileFields_butIgnoresEmailAndUsername() throws Exception {
        send(put("/api/v1/users/me"), accessToken, Map.of("fullname", "Trần Thị Bình",
                "phone", "0987654321", "email", "hacker@example.com", "username", "hacker"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.fullname").value("Trần Thị Bình"))
                .andExpect(jsonPath("$.data.phone").value("0987654321"))
                .andExpect(jsonPath("$.data.email").value("me.test@example.com"))
                .andExpect(jsonPath("$.data.username").value("me.test"));

        send(put("/api/v1/users/me"), accessToken, Map.of("fullname", ""))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.details.fullname").exists());
    }

    @Test
    void profile_isCreatedAtRegistration_andCanBeUpdated() throws Exception {
        send(get("/api/v1/users/me/profile"), accessToken, null)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.customerId").value(userId))
                .andExpect(jsonPath("$.data.loyaltyPoints").value(0));

        send(put("/api/v1/users/me/profile"), accessToken, profileBody("FEMALE", "1995-08-20"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.gender").value("FEMALE"))
                .andExpect(jsonPath("$.data.dateOfBirth").value("1995-08-20"))
                .andExpect(jsonPath("$.data.city").value("TP. Hồ Chí Minh"));

        send(put("/api/v1/users/me/profile"), accessToken, profileBody("UNKNOWN", "2999-01-01"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.details.gender").exists())
                .andExpect(jsonPath("$.error.details.dateOfBirth").exists());
    }

    @Test
    void profile_missing_returns404_andPutCreatesIt() throws Exception {
        customerProfileRepository.deleteById(userId);
        customerProfileRepository.flush();

        send(get("/api/v1/users/me/profile"), accessToken, null)
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error.code").value("CUSTOMER_PROFILE_NOT_FOUND"));

        send(put("/api/v1/users/me/profile"), accessToken, profileBody("MALE", "1990-01-01"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.customerId").value(userId));
        assertThat(customerProfileRepository.findById(userId)).isPresent();
    }

    @Test
    void changePassword_revokesSessions_andOnlyTheNewPasswordWorks() throws Exception {
        send(put("/api/v1/users/me/password"), accessToken,
                Map.of("currentPassword", "wrong-password", "newPassword", NEW_PASSWORD))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("INVALID_PASSWORD"));

        send(put("/api/v1/users/me/password"), accessToken,
                Map.of("currentPassword", PASSWORD, "newPassword", NEW_PASSWORD))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));

        // the refresh token from before the change no longer works
        send(post("/api/v1/auth/refresh"), null, Map.of("refreshToken", refreshToken))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error.code").value("INVALID_TOKEN"));

        send(post("/api/v1/auth/login"), null, Map.of("identifier", "me.test", "password", PASSWORD))
                .andExpect(status().isUnauthorized());
        send(post("/api/v1/auth/login"), null, Map.of("identifier", "me.test", "password", NEW_PASSWORD))
                .andExpect(status().isOk());
    }

    @Test
    void disabledAccount_isRejectedEvenWithAValidAccessToken() throws Exception {
        User user = userRepository.findById(userId).orElseThrow();
        user.setActive(false);
        userRepository.saveAndFlush(user);

        send(get("/api/v1/users/me"), accessToken, null)
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error.code").value("ACCOUNT_DISABLED"));
    }
}
