package com.example.Tech.controller.user;

import com.example.Tech.security.RefreshTokenService;
import com.example.Tech.service.upload.CloudinaryClient;
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
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

import java.util.Arrays;
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** With Cloudinary keys the avatar upload answers a Cloudinary URL the account can then save. */
@SpringBootTest(properties = {"app.upload.cloudinary.cloud-name=demo", "app.upload.cloudinary.api-key=key",
        "app.upload.cloudinary.api-secret=secret"})
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class AvatarCloudinaryIntegrationTest {

    private static final byte[] PNG = Arrays.copyOf(new byte[]{(byte) 0x89, 'P', 'N', 'G', '\r', '\n', 0x1A, '\n'}, 64);
    private static final String URL = "https://res.cloudinary.com/demo/image/upload/v1790000000/techshopping/avatars/"
            + "0123456789abcdef0123456789abcdef.png";

    @MockitoBean private CloudinaryClient cloudinaryClient;
    @Autowired private MockMvc mockMvc;
    @Autowired private JsonMapper jsonMapper;
    @Autowired private RefreshTokenService refreshTokenService;

    private Long userId;
    private String token;

    @BeforeEach
    void register() throws Exception {
        JsonNode data = jsonMapper.readTree(mockMvc.perform(post("/api/v1/auth/register").contentType(MediaType.APPLICATION_JSON)
                        .content(jsonMapper.writeValueAsString(Map.of("email", "avatar.cloud@example.com", "username", "avatar.cloud",
                                "password", "Matkhau@123", "fullname", "Ảnh Mây"))))
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString()).get("data");
        userId = data.get("user").get("id").asLong();
        token = data.get("accessToken").asString();
    }

    @AfterEach
    void revoke() {
        refreshTokenService.revokeAll(userId);
    }

    @Test
    void uploadGoesToCloudinary_andOnlyOurCloudinaryUrlsCanBeSaved() throws Exception {
        when(cloudinaryClient.upload(any(), anyString())).thenReturn(URL);

        mockMvc.perform(multipart("/api/v1/uploads/avatar").file(new MockMultipartFile("file", "me.png", "image/png", PNG))
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.url").value(URL));
        mockMvc.perform(put("/api/v1/users/me").header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonMapper.writeValueAsString(Map.of("fullname", "Ảnh Mây", "avatarUrl", URL))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.avatarUrl").value(URL));
        mockMvc.perform(put("/api/v1/users/me").header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonMapper.writeValueAsString(Map.of("fullname", "Ảnh Mây",
                                "avatarUrl", URL.replace("/demo/", "/someone/")))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.details.avatarUrl").exists());
    }
}
