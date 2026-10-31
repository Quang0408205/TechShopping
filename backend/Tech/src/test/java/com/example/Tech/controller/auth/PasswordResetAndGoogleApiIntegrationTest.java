package com.example.Tech.controller.auth;

import com.example.Tech.entity.user.User;
import com.example.Tech.entity.user.UserRole;
import com.example.Tech.exception.BusinessException;
import com.example.Tech.exception.ErrorCode;
import com.example.Tech.repository.mail.EmailLogRepository;
import com.example.Tech.repository.user.RoleRepository;
import com.example.Tech.repository.user.UserRepository;
import com.example.Tech.repository.user.UserRoleRepository;
import com.example.Tech.security.RefreshTokenService;
import com.example.Tech.service.auth.google.GoogleIdTokenVerifier;
import com.example.Tech.service.auth.google.GoogleIdentity;
import com.example.Tech.service.mail.MailMessage;
import com.example.Tech.service.mail.MailTransport;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Forgot / reset password and Google sign-in over HTTP; the mail transport and Google's token check are replaced. */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class PasswordResetAndGoogleApiIntegrationTest {

    private static final String PASSWORD = "Matkhau@123";
    private static final String NEW_PASSWORD = "MatkhauMoi@456";
    private static final Pattern LINK_TOKEN = Pattern.compile("reset-password\\.html\\?token=([A-Za-z0-9_-]+)");

    @Autowired private MockMvc mockMvc;
    @Autowired private JsonMapper jsonMapper;
    @Autowired private RefreshTokenService refreshTokenService;
    @Autowired private StringRedisTemplate redisTemplate;
    @Autowired private UserRepository userRepository;
    @Autowired private RoleRepository roleRepository;
    @Autowired private UserRoleRepository userRoleRepository;
    @Autowired private EmailLogRepository emailLogRepository;
    @Autowired private EntityManager entityManager;
    @MockitoBean private MailTransport mailTransport;
    @MockitoBean private GoogleIdTokenVerifier googleVerifier;

    private final List<Long> userIds = new ArrayList<>();

    @AfterEach
    void cleanRedis() {
        userIds.forEach(refreshTokenService::revokeAll);
        userIds.forEach(id -> redisTemplate.delete("auth:password-reset-user:" + id));
        Set<String> keys = redisTemplate.keys("auth:password-reset-email:*");
        if (keys != null && !keys.isEmpty()) {
            redisTemplate.delete(keys);
        }
    }

    @Test
    void forgotThenReset_changesThePassword_endsSessions_andTheLinkWorksOnce() throws Exception {
        JsonNode registered = register("reset.khach", "reset.khach@mail.com");
        String refreshToken = registered.get("refreshToken").asString();

        postJson("/api/v1/auth/forgot-password", Map.of("email", "Reset.Khach@Mail.com"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.success").value(true));
        String token = lastMailedToken();

        postJson("/api/v1/auth/reset-password", Map.of("token", token, "newPassword", NEW_PASSWORD))
                .andExpect(status().isOk());

        postJson("/api/v1/auth/login", Map.of("identifier", "reset.khach", "password", NEW_PASSWORD)).andExpect(status().isOk());
        postJson("/api/v1/auth/login", Map.of("identifier", "reset.khach", "password", PASSWORD))
                .andExpect(status().isUnauthorized());
        postJson("/api/v1/auth/refresh", Map.of("refreshToken", refreshToken)).andExpect(status().isUnauthorized());

        postJson("/api/v1/auth/reset-password", Map.of("token", token, "newPassword", "KhacNua@789"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("PASSWORD_RESET_TOKEN_INVALID"));
    }

    @Test
    void forgot_answersTheSameForUnknownAndStaffEmails_andSendsNothing() throws Exception {
        JsonNode staff = register("reset.staff", "reset.staff@mail.com");
        userRoleRepository.save(new UserRole(userRepository.getReferenceById(staff.get("user").get("id").asLong()),
                roleRepository.findByName("ADMIN").orElseThrow()));
        entityManager.flush();

        postJson("/api/v1/auth/forgot-password", Map.of("email", "khong.ton.tai@mail.com")).andExpect(status().isOk());
        postJson("/api/v1/auth/forgot-password", Map.of("email", "reset.staff@mail.com")).andExpect(status().isOk());
        postJson("/api/v1/auth/forgot-password", Map.of("email", "khong-phai-email")).andExpect(status().isBadRequest());

        verify(mailTransport, never()).send(any());
    }

    @Test
    void aNewRequestReplacesTheOldLink_andTheFourthRequestInTheWindowIsNotMailed() throws Exception {
        register("reset.nhieu", "reset.nhieu@mail.com");
        postJson("/api/v1/auth/forgot-password", Map.of("email", "reset.nhieu@mail.com")).andExpect(status().isOk());
        String first = lastMailedToken();
        reset(mailTransport);
        postJson("/api/v1/auth/forgot-password", Map.of("email", "reset.nhieu@mail.com")).andExpect(status().isOk());
        String second = lastMailedToken();
        assertThat(second).isNotEqualTo(first);

        postJson("/api/v1/auth/reset-password", Map.of("token", first, "newPassword", NEW_PASSWORD))
                .andExpect(status().isBadRequest());
        postJson("/api/v1/auth/reset-password", Map.of("token", second, "newPassword", NEW_PASSWORD))
                .andExpect(status().isOk());

        reset(mailTransport);
        postJson("/api/v1/auth/forgot-password", Map.of("email", "reset.nhieu@mail.com")).andExpect(status().isOk());
        postJson("/api/v1/auth/forgot-password", Map.of("email", "reset.nhieu@mail.com")).andExpect(status().isOk());
        verify(mailTransport, times(1)).send(any());
    }

    @Test
    void reset_rejectsAWeakPasswordAndAnUnknownToken() throws Exception {
        postJson("/api/v1/auth/reset-password", Map.of("token", "abc", "newPassword", "ngan"))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.error.code").value("VALIDATION_ERROR"));
        postJson("/api/v1/auth/reset-password", Map.of("token", "khong-co-token-nay", "newPassword", NEW_PASSWORD))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("PASSWORD_RESET_TOKEN_INVALID"));
    }

    @Test
    void google_createsACustomer_thenSignsInByTheSameGoogleAccount() throws Exception {
        when(googleVerifier.verify("good-token")).thenReturn(
                new GoogleIdentity("google-sub-1", "Nguoi.Moi@Gmail.com", true, "Người Mới", "https://lh3.example.com/a.png"));

        JsonNode first = data(postJson("/api/v1/auth/google", Map.of("credential", "good-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.user.email").value("nguoi.moi@gmail.com"))
                .andExpect(jsonPath("$.data.user.roles[0]").value("CUSTOMER")));
        userIds.add(first.get("user").get("id").asLong());

        User user = userRepository.findByEmail("nguoi.moi@gmail.com").orElseThrow();
        assertThat(user.getGoogleSub()).isEqualTo("google-sub-1");
        assertThat(user.getUsername()).isEqualTo("nguoi.moi");
        assertThat(user.getFullname()).isEqualTo("Người Mới");

        JsonNode second = data(postJson("/api/v1/auth/google", Map.of("credential", "good-token")).andExpect(status().isOk()));
        assertThat(second.get("user").get("id").asLong()).isEqualTo(user.getId());
        assertThat(userRepository.findAll().stream().filter(u -> "nguoi.moi@gmail.com".equals(u.getEmail()))).hasSize(1);
    }

    @Test
    void google_linksAnExistingAccountWithTheSameVerifiedEmail() throws Exception {
        JsonNode registered = register("co.san", "co.san@gmail.com");
        when(googleVerifier.verify("link-token")).thenReturn(
                new GoogleIdentity("google-sub-2", "co.san@gmail.com", true, "Có Sẵn", null));

        JsonNode auth = data(postJson("/api/v1/auth/google", Map.of("credential", "link-token")).andExpect(status().isOk()));

        assertThat(auth.get("user").get("id").asLong()).isEqualTo(registered.get("user").get("id").asLong());
        assertThat(userRepository.findByUsername("co.san").orElseThrow().getGoogleSub()).isEqualTo("google-sub-2");
    }

    @Test
    void google_refusesAnotherGoogleAccountOnALinkedEmail_anUnverifiedEmail_staff_andABadToken() throws Exception {
        register("da.lien.ket", "da.lien.ket@gmail.com");
        when(googleVerifier.verify("owner")).thenReturn(new GoogleIdentity("sub-owner", "da.lien.ket@gmail.com", true, "A", null));
        postJson("/api/v1/auth/google", Map.of("credential", "owner")).andExpect(status().isOk());
        when(googleVerifier.verify("intruder")).thenReturn(new GoogleIdentity("sub-intruder", "da.lien.ket@gmail.com", true, "B", null));
        postJson("/api/v1/auth/google", Map.of("credential", "intruder"))
                .andExpect(status().isUnauthorized()).andExpect(jsonPath("$.error.code").value("GOOGLE_TOKEN_INVALID"));

        when(googleVerifier.verify("unverified")).thenReturn(new GoogleIdentity("sub-u", "chua.xac.minh@gmail.com", false, "C", null));
        postJson("/api/v1/auth/google", Map.of("credential", "unverified"))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.error.code").value("GOOGLE_EMAIL_NOT_VERIFIED"));
        assertThat(userRepository.findByEmail("chua.xac.minh@gmail.com")).isEmpty();

        JsonNode staff = register("google.staff", "google.staff@gmail.com");
        userRoleRepository.save(new UserRole(userRepository.getReferenceById(staff.get("user").get("id").asLong()),
                roleRepository.findByName("STAFF").orElseThrow()));
        entityManager.flush();
        when(googleVerifier.verify("staff")).thenReturn(new GoogleIdentity("sub-staff", "google.staff@gmail.com", true, "D", null));
        postJson("/api/v1/auth/google", Map.of("credential", "staff"))
                .andExpect(status().isForbidden()).andExpect(jsonPath("$.error.code").value("GOOGLE_LOGIN_NOT_ALLOWED"));
        assertThat(userRepository.findByUsername("google.staff").orElseThrow().getGoogleSub()).isNull();

        when(googleVerifier.verify("forged")).thenThrow(new BusinessException(ErrorCode.GOOGLE_TOKEN_INVALID));
        postJson("/api/v1/auth/google", Map.of("credential", "forged"))
                .andExpect(status().isUnauthorized()).andExpect(jsonPath("$.error.code").value("GOOGLE_TOKEN_INVALID"));
        postJson("/api/v1/auth/google", Map.of()).andExpect(status().isBadRequest());
    }

    @Test
    void google_pickAFreeUsernameWhenTheEmailNameIsTakenOrShort() throws Exception {
        register("trung.ten", "mot.nguoi.khac@mail.com");
        when(googleVerifier.verify("t1")).thenReturn(new GoogleIdentity("sub-t1", "trung.ten@gmail.com", true, "T", null));
        when(googleVerifier.verify("t2")).thenReturn(new GoogleIdentity("sub-t2", "ab@gmail.com", true, "S", null));

        userIds.add(data(postJson("/api/v1/auth/google", Map.of("credential", "t1")).andExpect(status().isOk()))
                .get("user").get("id").asLong());
        userIds.add(data(postJson("/api/v1/auth/google", Map.of("credential", "t2")).andExpect(status().isOk()))
                .get("user").get("id").asLong());

        String taken = userRepository.findByEmail("trung.ten@gmail.com").orElseThrow().getUsername();
        assertThat(taken).startsWith("trung.ten").isNotEqualTo("trung.ten");
        assertThat(userRepository.findByEmail("ab@gmail.com").orElseThrow().getUsername()).hasSizeGreaterThanOrEqualTo(3);
    }

    @Test
    void googleConfig_isPublic() throws Exception {
        mockMvc.perform(get("/api/v1/auth/google/config")).andExpect(status().isOk())
                .andExpect(jsonPath("$.data.enabled").isBoolean());
    }

    private String lastMailedToken() throws Exception {
        ArgumentCaptor<MailMessage> captor = ArgumentCaptor.forClass(MailMessage.class);
        verify(mailTransport, org.mockito.Mockito.atLeastOnce()).send(captor.capture());
        Matcher matcher = LINK_TOKEN.matcher(captor.getValue().text());
        assertThat(matcher.find()).as("link with a token in the mail").isTrue();
        assertThat(captor.getValue().html()).contains(matcher.group(1));
        return matcher.group(1);
    }

    private JsonNode register(String username, String email) throws Exception {
        JsonNode data = data(postJson("/api/v1/auth/register", Map.of("email", email, "username", username,
                "password", PASSWORD, "fullname", "Khách Thử", "phone", "0901234567")).andExpect(status().isCreated()));
        userIds.add(data.get("user").get("id").asLong());
        return data;
    }

    private ResultActions postJson(String path, Object body) throws Exception {
        return mockMvc.perform(post(path).contentType(MediaType.APPLICATION_JSON)
                .content(jsonMapper.writeValueAsString(body)));
    }

    private JsonNode data(ResultActions result) throws Exception {
        return jsonMapper.readTree(result.andReturn().getResponse().getContentAsString()).get("data");
    }
}
