package com.example.Tech.controller.user;

import com.example.Tech.entity.user.Role;
import com.example.Tech.entity.user.User;
import com.example.Tech.entity.user.UserRole;
import com.example.Tech.repository.user.RoleRepository;
import com.example.Tech.repository.user.UserRepository;
import com.example.Tech.repository.user.UserRoleRepository;
import com.example.Tech.security.JwtTokenService;
import com.example.Tech.security.RefreshTokenService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * /api/v1/admin/users over HTTP against the test database (rolled back) and the real Redis
 * (all refresh tokens of the users created here are revoked afterwards).
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class AdminUserApiIntegrationTest {

    private static final String PASSWORD = "Matkhau@123";
    private static final String BASE = "/api/v1/admin/users";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JsonMapper jsonMapper;

    @Autowired
    private JwtTokenService jwtTokenService;

    @Autowired
    private JwtDecoder jwtDecoder;

    @Autowired
    private RefreshTokenService refreshTokenService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private UserRoleRepository userRoleRepository;

    private final List<Long> createdUserIds = new ArrayList<>();

    private User admin;
    private String adminToken;
    private Long customerId;
    private String customerToken;
    private String customerRefresh;

    @BeforeEach
    void setUp() throws Exception {
        admin = saveAdmin("admin.api.test");
        adminToken = jwtTokenService.issueAccessToken(admin, List.of("ADMIN"));

        JsonNode customer = register("cust.api.test", "Khách Hàng Một");
        customerId = customer.get("user").get("id").asLong();
        customerToken = customer.get("accessToken").asString();
        customerRefresh = customer.get("refreshToken").asString();
    }

    @AfterEach
    void revokeTokens() {
        createdUserIds.forEach(refreshTokenService::revokeAll);
    }

    private User saveAdmin(String username) {
        User user = new User();
        user.setEmail(username + "@example.com");
        user.setUsername(username);
        user.setFullname("Quản Trị Test");
        user.setPasswordHash("{bcrypt}not-used");
        User saved = userRepository.saveAndFlush(user);
        Role adminRole = roleRepository.findByName("ADMIN").orElseThrow();
        userRoleRepository.saveAndFlush(new UserRole(saved, adminRole));
        createdUserIds.add(saved.getId());
        return saved;
    }

    private JsonNode register(String username, String fullname) throws Exception {
        JsonNode data = data(send(post("/api/v1/auth/register"), null, Map.of("email", username + "@example.com",
                "username", username, "password", PASSWORD, "fullname", fullname))
                .andExpect(status().isCreated()));
        createdUserIds.add(data.get("user").get("id").asLong());
        return data;
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

    private ResultActions login(String username) throws Exception {
        return send(post("/api/v1/auth/login"), null, Map.of("identifier", username, "password", PASSWORD));
    }

    // ---------- access ----------

    @Test
    void customer_cannotUseAdminEndpoints() throws Exception {
        send(get(BASE), customerToken, null)
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error.code").value("ACCESS_DENIED"));
        send(get(BASE), null, null).andExpect(status().isUnauthorized());
    }

    @Test
    void adminDemotedInDatabase_isDeniedDespiteAdminToken() throws Exception {
        userRoleRepository.deleteAll(userRoleRepository.findAllByIdUserId(admin.getId()));
        userRoleRepository.flush();

        send(get(BASE), adminToken, null)
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error.code").value("ACCESS_DENIED"));
    }

    // ---------- list / get ----------

    @Test
    void search_filtersAndPaginates_withRoles() throws Exception {
        register("cust.api.second", "Khách Hàng Hai");

        send(get(BASE).param("keyword", "api.test").param("sort", "id"), adminToken, null)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalElements").value(2))
                .andExpect(jsonPath("$.data.content[0].username").value("admin.api.test"))
                .andExpect(jsonPath("$.data.content[0].roles[0]").value("ADMIN"))
                .andExpect(jsonPath("$.data.content[1].username").value("cust.api.test"))
                .andExpect(jsonPath("$.data.content[1].roles[0]").value("CUSTOMER"));

        send(get(BASE).param("keyword", "KHÁCH HÀNG").param("role", "customer").param("size", "1"), adminToken, null)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.size").value(1))
                .andExpect(jsonPath("$.data.totalElements").value(2))
                .andExpect(jsonPath("$.data.totalPages").value(2));

        send(get(BASE).param("keyword", "api").param("role", "ADMIN"), adminToken, null)
                .andExpect(jsonPath("$.data.content[*].username", hasItem("admin.api.test")))
                .andExpect(jsonPath("$.data.content[*].username", not(hasItem("cust.api.test"))));

        send(get(BASE).param("sort", "unknownField"), adminToken, null)
                .andExpect(status().isBadRequest());
    }

    @Test
    void getById_returnsUser_or404() throws Exception {
        send(get(BASE + "/" + customerId), adminToken, null)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.email").value("cust.api.test@example.com"))
                .andExpect(jsonPath("$.data.passwordHash").doesNotExist());
        send(get(BASE + "/999999999"), adminToken, null)
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error.code").value("USER_NOT_FOUND"));
    }

    // ---------- status ----------

    @Test
    void deactivate_blocksLoginRefreshAndMe_andReactivateRestoresLogin() throws Exception {
        send(patch(BASE + "/" + customerId + "/status"), adminToken, Map.of("active", false))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.isActive").value(false));

        login("cust.api.test").andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error.code").value("ACCOUNT_DISABLED"));
        send(post("/api/v1/auth/refresh"), null, Map.of("refreshToken", customerRefresh))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error.code").value("INVALID_TOKEN"));
        send(get("/api/v1/users/me"), customerToken, null)
                .andExpect(status().isForbidden());

        send(patch(BASE + "/" + customerId + "/status"), adminToken, Map.of("active", true))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.isActive").value(true));
        login("cust.api.test").andExpect(status().isOk());
    }

    @Test
    void status_validationAndSelfProtection() throws Exception {
        send(patch(BASE + "/" + customerId + "/status"), adminToken, Map.of())
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.details.active").exists());
        send(patch(BASE + "/" + admin.getId() + "/status"), adminToken, Map.of("active", false))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error.code").value("CANNOT_MODIFY_OWN_ACCOUNT"));
    }

    // ---------- roles ----------

    @Test
    void updateRoles_appliesToTheNextLoginToken() throws Exception {
        send(put(BASE + "/" + customerId + "/roles"), adminToken, Map.of("roles", List.of("staff", "CUSTOMER")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.roles[0]").value("CUSTOMER"))
                .andExpect(jsonPath("$.data.roles[1]").value("STAFF"));

        JsonNode loggedIn = data(login("cust.api.test").andExpect(status().isOk()));
        assertThat(jwtDecoder.decode(loggedIn.get("accessToken").asString()).getClaimAsStringList("roles"))
                .containsExactly("CUSTOMER", "STAFF");
    }

    @Test
    void updateRoles_errors() throws Exception {
        send(put(BASE + "/" + customerId + "/roles"), adminToken, Map.of("roles", List.of()))
                .andExpect(status().isBadRequest());
        send(put(BASE + "/" + customerId + "/roles"), adminToken, Map.of("roles", List.of("SUPERUSER")))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error.code").value("ROLE_NOT_FOUND"));
        send(put(BASE + "/" + admin.getId() + "/roles"), adminToken, Map.of("roles", List.of("CUSTOMER")))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error.code").value("CANNOT_MODIFY_OWN_ACCOUNT"));
    }

    // ---------- delete ----------

    @Test
    void delete_softDeletes_hidesFromDefaultList_andBlocksTheAccount() throws Exception {
        send(delete(BASE + "/" + customerId), adminToken, null).andExpect(status().isNoContent());
        send(delete(BASE + "/" + customerId), adminToken, null).andExpect(status().isNoContent());

        User deleted = userRepository.findById(customerId).orElseThrow();
        assertThat(deleted.getDeletedAt()).isNotNull();
        assertThat(deleted.getActive()).isFalse();

        send(get(BASE).param("keyword", "cust.api.test"), adminToken, null)
                .andExpect(jsonPath("$.data.totalElements").value(0));
        send(get(BASE).param("keyword", "cust.api.test").param("includeDeleted", "true"), adminToken, null)
                .andExpect(jsonPath("$.data.totalElements").value(1));
        send(get(BASE + "/" + customerId), adminToken, null)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.isActive").value(false));

        login("cust.api.test").andExpect(status().isForbidden());
        send(post("/api/v1/auth/refresh"), null, Map.of("refreshToken", customerRefresh))
                .andExpect(status().isUnauthorized());
        send(patch(BASE + "/" + customerId + "/status"), adminToken, Map.of("active", true))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error.code").value("USER_DELETED"));
        send(delete(BASE + "/" + admin.getId()), adminToken, null)
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error.code").value("CANNOT_MODIFY_OWN_ACCOUNT"));
    }
}
