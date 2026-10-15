package com.example.Tech.controller.user;

import com.example.Tech.security.RefreshTokenService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Kế hoạch v2 GĐ5: POST /api/v1/uploads/avatar, PUT /users/me with an uploaded avatar only, and avatarUrl in the
 * login / refresh response (test database, rolled back; files go to the test upload folder).
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class AvatarApiIntegrationTest {

    private static final String PASSWORD = "Matkhau@123";
    private static final byte[] PNG = Arrays.copyOf(new byte[]{(byte) 0x89, 'P', 'N', 'G', '\r', '\n', 0x1A, '\n'}, 64);
    private static final byte[] GIF = "GIF89a-not-accepted".getBytes();

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JsonMapper jsonMapper;

    @Autowired
    private RefreshTokenService refreshTokenService;

    private Long userId;
    private String accessToken;

    @BeforeEach
    void registerUser() throws Exception {
        JsonNode data = data(send(post("/api/v1/auth/register"), null, Map.of(
                "email", "avatar.test@example.com", "username", "avatar.test", "password", PASSWORD,
                "fullname", "Nguyễn Văn Ảnh"))
                .andExpect(status().isCreated()));
        userId = data.get("user").get("id").asLong();
        accessToken = data.get("accessToken").asString();
        assertThat(data.get("user").has("avatarUrl")).isTrue();
        assertThat(data.get("user").get("avatarUrl").isNull()).isTrue();
    }

    @AfterEach
    void revokeTokens() {
        refreshTokenService.revokeAll(userId);
    }

    @Test
    void uploadThenSave_showsUpInMe_loginAndRefresh() throws Exception {
        String url = upload(new MockMultipartFile("file", "me.png", "image/png", PNG))
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        String avatar = jsonMapper.readTree(url).get("data").get("url").asString();
        assertThat(avatar).contains("/uploads/avatars/").endsWith(".png");

        send(put("/api/v1/users/me"), accessToken, me(avatar))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.avatarUrl").value(avatar));
        send(get("/api/v1/users/me"), accessToken, null)
                .andExpect(jsonPath("$.data.avatarUrl").value(avatar));

        JsonNode login = data(send(post("/api/v1/auth/login"), null, Map.of("identifier", "avatar.test", "password", PASSWORD))
                .andExpect(status().isOk()));
        assertThat(login.get("user").get("avatarUrl").asString()).isEqualTo(avatar);
        JsonNode refreshed = data(send(post("/api/v1/auth/refresh"), null, Map.of("refreshToken", login.get("refreshToken").asString()))
                .andExpect(status().isOk()));
        assertThat(refreshed.get("user").get("avatarUrl").asString()).isEqualTo(avatar);

        // removing it again is allowed
        send(put("/api/v1/users/me"), accessToken, me(null))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.avatarUrl").doesNotExist());
    }

    @Test
    void anAvatarFromAnotherSite_isRefused() throws Exception {
        send(put("/api/v1/users/me"), accessToken, me("https://evil.example/x.png"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.details.avatarUrl").exists());
        // our prefix but a file we never stored
        send(put("/api/v1/users/me"), accessToken, me("http://localhost:8080/uploads/avatars/00000000-0000-0000-0000-000000000000.png"))
                .andExpect(status().isBadRequest());
        // a review photo is not an avatar
        send(put("/api/v1/users/me"), accessToken, me("http://localhost:8080/uploads/reviews/00000000-0000-0000-0000-000000000000.png"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void upload_rules() throws Exception {
        mockMvc.perform(multipart("/api/v1/uploads/avatar").file(new MockMultipartFile("file", "me.png", "image/png", PNG)))
                .andExpect(status().isUnauthorized());
        upload(new MockMultipartFile("file", "me.gif", "image/gif", GIF))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("INVALID_IMAGE_FILE"));
        byte[] big = Arrays.copyOf(PNG, 2 * 1024 * 1024 + 1);
        upload(new MockMultipartFile("file", "big.png", "image/png", big))
                .andExpect(status().isContentTooLarge())
                .andExpect(jsonPath("$.error.code").value("IMAGE_TOO_LARGE"))
                .andExpect(jsonPath("$.error.message").value("Ảnh đại diện tối đa 2 MB"));
    }

    private ResultActions upload(MockMultipartFile file) throws Exception {
        return mockMvc.perform(multipart("/api/v1/uploads/avatar").file(file)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken));
    }

    private static Map<String, Object> me(String avatarUrl) {
        Map<String, Object> body = new HashMap<>();
        body.put("fullname", "Nguyễn Văn Ảnh");
        body.put("avatarUrl", avatarUrl);
        return body;
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
}
