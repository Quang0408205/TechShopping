package com.example.Tech.controller.contact;

import com.example.Tech.entity.store.Store;
import com.example.Tech.entity.user.UserRole;
import com.example.Tech.repository.user.RoleRepository;
import com.example.Tech.repository.user.UserRepository;
import com.example.Tech.repository.user.UserRoleRepository;
import com.example.Tech.security.RefreshTokenService;
import com.example.Tech.support.StoreFixtures;
import jakarta.persistence.EntityManager;
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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Contact form (kiemthu GĐ7) over HTTP against the test database (rolled back). Redis counters are real, so every
 * run uses fresh emails; the per-IP limit is raised in the test profile (all requests come from 127.0.0.1).
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class ContactRequestApiIntegrationTest {

    private static final String PASSWORD = "Matkhau@123";
    private static final String PUBLIC_URL = "/api/v1/contact-requests";
    private static final String ADMIN_URL = "/api/v1/admin/contact-requests";

    @Autowired private MockMvc mockMvc;
    @Autowired private JsonMapper jsonMapper;
    @Autowired private RefreshTokenService refreshTokenService;
    @Autowired private UserRepository userRepository;
    @Autowired private RoleRepository roleRepository;
    @Autowired private UserRoleRepository userRoleRepository;
    @Autowired private EntityManager entityManager;

    private final List<Long> registeredUserIds = new ArrayList<>();
    private final String run = Long.toString(System.nanoTime(), 36);

    private Long customerId;
    private String customerToken;
    private String staffToken;
    private String managerToken;
    private String staffWithoutStoreToken;
    private String adminToken;

    @BeforeEach
    void setUp() throws Exception {
        JsonNode customer = register("ctr.khach", "Khách Liên Hệ");
        customerId = customer.get("user").get("id").asLong();
        customerToken = customer.get("accessToken").asString();
        staffToken = registerWithRole("ctr.nv", "Nhân Viên Liên Hệ", "STAFF");
        Long staffId = registeredUserIds.getLast();
        managerToken = registerWithRole("ctr.ql", "Quản Lý Liên Hệ", "BRANCH_MANAGER");
        Long managerId = registeredUserIds.getLast();
        staffWithoutStoreToken = registerWithRole("ctr.nv2", "Nhân Viên Chưa Gán", "STAFF");
        adminToken = registerWithRole("ctr.admin", "Quản Trị Liên Hệ", "ADMIN");

        Store storeA = StoreFixtures.store(entityManager, "ZZ CN Liên Hệ A", "Quận 3");
        Store storeB = StoreFixtures.store(entityManager, "ZZ CN Liên Hệ B", "Quận 7");
        StoreFixtures.assign(entityManager, staffId, storeA);
        StoreFixtures.assign(entityManager, managerId, storeB, "Quản lý chi nhánh");
    }

    @AfterEach
    void revokeTokens() {
        registeredUserIds.forEach(refreshTokenService::revokeAll);
    }

    @Test
    void guestAndCustomer_send_201_customerLinked() throws Exception {
        JsonNode guest = data(send(post(PUBLIC_URL), null, form("khach" + run + "@example.com"))
                .andExpect(status().isCreated()));
        assertThat(guest.get("id").asLong()).isPositive();
        assertThat(guest.get("createdAt").isNull()).isFalse();

        Map<String, Object> body = form("Khach.Mua" + run + "@Example.com");
        body.put("phone", " 0912345678 ");
        body.put("topic", "PAYMENT");
        body.put("message", "   Tôi muốn hỏi về trả góp 0% cho đơn 5 triệu.   ");
        data(send(post(PUBLIC_URL), customerToken, body).andExpect(status().isCreated()));

        JsonNode page = data(send(get(ADMIN_URL).param("keyword", "khach.mua" + run), staffToken, null)
                .andExpect(status().isOk()));
        assertThat(page.get("totalElements").asLong()).isEqualTo(1);
        JsonNode row = page.get("content").get(0);
        assertThat(row.get("userId").asLong()).isEqualTo(customerId);
        assertThat(row.get("username").asString()).isEqualTo("ctr.khach");
        assertThat(row.get("email").asString()).isEqualTo("khach.mua" + run + "@example.com");
        assertThat(row.get("phone").asString()).isEqualTo("0912345678");
        assertThat(row.get("topic").asString()).isEqualTo("PAYMENT");
        assertThat(row.get("message").asString()).isEqualTo("Tôi muốn hỏi về trả góp 0% cho đơn 5 triệu.");
        assertThat(row.get("status").asString()).isEqualTo("NEW");
        assertThat(row.get("handledByName").isNull()).isTrue();

        JsonNode guestRow = data(send(get(ADMIN_URL).param("keyword", "khach" + run + "@"), staffToken, null))
                .get("content").get(0);
        assertThat(guestRow.get("userId").isNull()).isTrue();
        assertThat(guestRow.get("phone").isNull()).isTrue();
    }

    @Test
    void send_validation_perField() throws Exception {
        Map<String, Object> body = new HashMap<>();
        body.put("fullName", "A");
        body.put("email", "khong-phai-email");
        body.put("phone", "12345");
        body.put("message", "ngắn");
        send(post(PUBLIC_URL), null, body)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.error.details.fullName").exists())
                .andExpect(jsonPath("$.error.details.email").exists())
                .andExpect(jsonPath("$.error.details.phone").exists())
                .andExpect(jsonPath("$.error.details.topic").exists())
                .andExpect(jsonPath("$.error.details.message").exists());

        // 10+ characters only thanks to the spaces: checked again after trimming
        Map<String, Object> padded = form("pad" + run + "@example.com");
        padded.put("message", "   ngắn       ");
        send(post(PUBLIC_URL), null, padded)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.details.message").exists());
    }

    @Test
    void send_overEmailLimit_429() throws Exception {
        String email = "spam" + run + "@example.com";
        for (int i = 0; i < 3; i++) {
            send(post(PUBLIC_URL), null, form(email)).andExpect(status().isCreated());
        }
        send(post(PUBLIC_URL), null, form(email.toUpperCase()))
                .andExpect(status().isTooManyRequests())
                .andExpect(jsonPath("$.error.code").value("TOO_MANY_REQUESTS"))
                .andExpect(jsonPath("$.error.message").value(org.hamcrest.Matchers.containsString("thử lại sau")));
        send(post(PUBLIC_URL), null, form("other" + run + "@example.com")).andExpect(status().isCreated());
    }

    @Test
    void access_byRole() throws Exception {
        send(get(ADMIN_URL), null, null).andExpect(status().isUnauthorized());
        send(get(ADMIN_URL), customerToken, null).andExpect(status().isForbidden());
        send(get(ADMIN_URL), staffWithoutStoreToken, null)
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error.code").value("NO_ACTIVE_STORE_ASSIGNMENT"));
        send(get(ADMIN_URL), staffToken, null).andExpect(status().isOk());
        send(get(ADMIN_URL), managerToken, null).andExpect(status().isOk());
        send(get(ADMIN_URL), adminToken, null).andExpect(status().isOk());

        long id = create("access" + run + "@example.com");
        Map<String, Object> change = Map.of("status", "IN_PROGRESS");
        send(patch(ADMIN_URL + "/" + id), customerToken, change).andExpect(status().isForbidden());
        send(patch(ADMIN_URL + "/" + id), adminToken, change)
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error.code").value("ADMIN_READ_ONLY"));
        send(patch(ADMIN_URL + "/" + id), staffWithoutStoreToken, change)
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error.code").value("NO_ACTIVE_STORE_ASSIGNMENT"));
        // one shared inbox: the manager of another store handles it too
        send(patch(ADMIN_URL + "/" + id), managerToken, change).andExpect(status().isOk());
        send(patch(ADMIN_URL + "/999999999"), staffToken, change)
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error.code").value("CONTACT_REQUEST_NOT_FOUND"));
    }

    @Test
    void statusFlow_noteRequiredToResolve_reopen_neverBackToNew() throws Exception {
        long id = create("flow" + run + "@example.com");
        String url = ADMIN_URL + "/" + id;

        send(patch(url), staffToken, Map.of("status", "RESOLVED"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.details.staffNote").exists());

        JsonNode inProgress = data(send(patch(url), staffToken, Map.of("status", "IN_PROGRESS"))
                .andExpect(status().isOk()));
        assertThat(inProgress.get("status").asString()).isEqualTo("IN_PROGRESS");
        assertThat(inProgress.get("handledByName").asString()).isEqualTo("Nhân Viên Liên Hệ");
        assertThat(inProgress.get("handledAt").isNull()).isFalse();

        JsonNode resolved = data(send(patch(url), managerToken,
                Map.of("status", "RESOLVED", "staffNote", "  Đã gọi lại cho khách  ")).andExpect(status().isOk()));
        assertThat(resolved.get("status").asString()).isEqualTo("RESOLVED");
        assertThat(resolved.get("staffNote").asString()).isEqualTo("Đã gọi lại cho khách");
        assertThat(resolved.get("handledByName").asString()).isEqualTo("Quản Lý Liên Hệ");

        send(patch(url), staffToken, Map.of("status", "NEW"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error.code").value("INVALID_CONTACT_STATUS"));

        // reopen keeps the note; resolving again without a new note reuses it
        JsonNode reopened = data(send(patch(url), staffToken, Map.of("status", "IN_PROGRESS"))
                .andExpect(status().isOk()));
        assertThat(reopened.get("staffNote").asString()).isEqualTo("Đã gọi lại cho khách");
        send(patch(url), staffToken, Map.of("status", "RESOLVED")).andExpect(status().isOk());
    }

    @Test
    void search_filters_newestFirst() throws Exception {
        long first = create("loc1" + run + "@example.com");
        Map<String, Object> other = form("loc2" + run + "@example.com");
        other.put("topic", "AFTER_SALES");
        long second = data(send(post(PUBLIC_URL), null, other).andExpect(status().isCreated())).get("id").asLong();
        send(patch(ADMIN_URL + "/" + first), staffToken, Map.of("status", "IN_PROGRESS")).andExpect(status().isOk());

        JsonNode all = data(send(get(ADMIN_URL).param("keyword", run), adminToken, null));
        assertThat(all.get("totalElements").asLong()).isEqualTo(2);
        assertThat(all.get("content").get(0).get("id").asLong()).isEqualTo(second);

        JsonNode afterSales = data(send(get(ADMIN_URL).param("keyword", run).param("topic", "AFTER_SALES"),
                staffToken, null));
        assertThat(afterSales.get("totalElements").asLong()).isEqualTo(1);
        assertThat(afterSales.get("content").get(0).get("id").asLong()).isEqualTo(second);

        JsonNode inProgress = data(send(get(ADMIN_URL).param("keyword", run).param("status", "IN_PROGRESS"),
                staffToken, null));
        assertThat(inProgress.get("totalElements").asLong()).isEqualTo(1);
        assertThat(inProgress.get("content").get(0).get("id").asLong()).isEqualTo(first);
    }

    @Test
    void dashboard_countsOpenContactRequests() throws Exception {
        long before = data(send(get("/api/v1/admin/dashboard/summary"), staffToken, null)
                .andExpect(status().isOk())).get("openContactRequests").asLong();
        long id = create("dash" + run + "@example.com");
        assertThat(data(send(get("/api/v1/admin/dashboard/summary"), managerToken, null))
                .get("openContactRequests").asLong()).isEqualTo(before + 1);
        send(patch(ADMIN_URL + "/" + id), staffToken, Map.of("status", "RESOLVED", "staffNote", "Xong"))
                .andExpect(status().isOk());
        assertThat(data(send(get("/api/v1/admin/dashboard/summary"), adminToken, null))
                .get("openContactRequests").asLong()).isEqualTo(before);
    }

    private Map<String, Object> form(String email) {
        Map<String, Object> body = new HashMap<>();
        body.put("fullName", "Khách Liên Hệ " + run);
        body.put("email", email);
        body.put("topic", "ORDER");
        body.put("message", "Đơn hàng của tôi khi nào được giao vậy shop?");
        return body;
    }

    private long create(String email) throws Exception {
        return data(send(post(PUBLIC_URL), null, form(email)).andExpect(status().isCreated())).get("id").asLong();
    }

    private JsonNode register(String username, String fullname) throws Exception {
        JsonNode data = data(send(post("/api/v1/auth/register"), null, Map.of(
                "email", username + "@example.com", "username", username, "password", PASSWORD,
                "fullname", fullname)).andExpect(status().isCreated()));
        registeredUserIds.add(data.get("user").get("id").asLong());
        return data;
    }

    private String registerWithRole(String username, String fullname, String role) throws Exception {
        Long id = register(username, fullname).get("user").get("id").asLong();
        userRoleRepository.save(new UserRole(userRepository.getReferenceById(id),
                roleRepository.findByName(role).orElseThrow()));
        entityManager.flush();
        return data(send(post("/api/v1/auth/login"), null, Map.of("identifier", username, "password", PASSWORD))
                .andExpect(status().isOk())).get("accessToken").asString();
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
