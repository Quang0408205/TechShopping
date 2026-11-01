package com.example.Tech.controller.content;

import com.example.Tech.entity.user.UserRole;
import com.example.Tech.repository.user.RoleRepository;
import com.example.Tech.repository.user.UserRepository;
import com.example.Tech.repository.user.UserRoleRepository;
import com.example.Tech.security.RefreshTokenService;
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

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Home banners and static pages over HTTP against the test database (rolled back). */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class SiteContentApiIntegrationTest {

    private static final String PASSWORD = "Matkhau@123";

    @Autowired private MockMvc mockMvc;
    @Autowired private JsonMapper jsonMapper;
    @Autowired private RefreshTokenService refreshTokenService;
    @Autowired private UserRepository userRepository;
    @Autowired private RoleRepository roleRepository;
    @Autowired private UserRoleRepository userRoleRepository;
    @Autowired private EntityManager entityManager;

    private final List<Long> registeredUserIds = new ArrayList<>();

    private String adminToken;
    private String staffToken;
    private String customerToken;

    @BeforeEach
    void setUp() throws Exception {
        customerToken = register("ct.customer").get("accessToken").asString();
        adminToken = registerWithRole("ct.admin", "ADMIN");
        staffToken = registerWithRole("ct.staff", "STAFF");
    }

    @AfterEach
    void revokeTokens() {
        registeredUserIds.forEach(refreshTokenService::revokeAll);
    }

    @Test
    void visitorsSeeOnlyBannersThatAreActiveAndInsideTheirWindow_inDisplayOrder() throws Exception {
        LocalDateTime now = LocalDateTime.now();
        banner("Hai", 2, true, null, null);
        banner("Một", 1, true, now.minusDays(1), now.plusDays(1));
        banner("Tắt", 0, false, null, null);
        banner("Chưa tới", 0, true, now.plusDays(1), now.plusDays(2));
        banner("Hết hạn", 0, true, now.minusDays(2), now.minusDays(1));

        JsonNode shown = data(send(get("/api/v1/banners"), null, null).andExpect(status().isOk()));
        List<String> titles = new ArrayList<>();
        shown.forEach(node -> titles.add(node.get("title").asString()));
        assertThat(titles).containsSubsequence("Một", "Hai").doesNotContain("Tắt", "Chưa tới", "Hết hạn");

        JsonNode all = data(send(get("/api/v1/admin/banners"), adminToken, null).andExpect(status().isOk()));
        assertThat(all.toString()).contains("Tắt").contains("Chưa tới").contains("Hết hạn");
    }

    @Test
    void bannerFieldsAreChecked_andOnlyAnAdminWritesThem() throws Exception {
        Map<String, Object> good = bannerBody("Mùa lễ", 1, true, null, null);
        send(post("/api/v1/admin/banners"), null, good).andExpect(status().isUnauthorized());
        send(post("/api/v1/admin/banners"), customerToken, good).andExpect(status().isForbidden());
        send(post("/api/v1/admin/banners"), staffToken, good).andExpect(status().isForbidden());
        send(get("/api/v1/admin/banners"), staffToken, null).andExpect(status().isForbidden());

        send(post("/api/v1/admin/banners"), adminToken, withField(good, "title", " ")).andExpect(status().isBadRequest());
        send(post("/api/v1/admin/banners"), adminToken, withField(good, "imageUrl", "javascript:alert(1)")).andExpect(status().isBadRequest());
        send(post("/api/v1/admin/banners"), adminToken, withField(good, "linkUrl", "//evil.example/x")).andExpect(status().isBadRequest());
        send(post("/api/v1/admin/banners"), adminToken, withField(good, "linkUrl", "javascript:alert(1)")).andExpect(status().isBadRequest());
        send(post("/api/v1/admin/banners"), adminToken, withField(good, "linkUrl", "https://poy.vn/sale")).andExpect(status().isCreated());
        send(post("/api/v1/admin/banners"), adminToken, withField(good, "linkUrl", "customer/products.html?categoryId=3")).andExpect(status().isCreated());
        LocalDateTime now = LocalDateTime.now();
        send(post("/api/v1/admin/banners"), adminToken, bannerBody("Ngược", 1, true, now.plusDays(2), now.plusDays(1)))
                .andExpect(status().isBadRequest());

        int id = data(send(post("/api/v1/admin/banners"), adminToken, good).andExpect(status().isCreated())).get("id").asInt();
        send(put("/api/v1/admin/banners/" + id), adminToken, withField(good, "title", "Đã sửa")).andExpect(status().isOk());
        assertThat(data(send(get("/api/v1/banners"), null, null)).toString()).contains("Đã sửa");
        send(delete("/api/v1/admin/banners/" + id), adminToken, null).andExpect(status().isOk());
        send(delete("/api/v1/admin/banners/" + id), adminToken, null).andExpect(status().isNotFound());
        send(put("/api/v1/admin/banners/" + id), adminToken, good).andExpect(status().isNotFound());
    }

    @Test
    void aPageBodyIsFilteredWhenSaved_andServedPublishedOnly() throws Exception {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("title", "Trang thử");
        body.put("body", "<h2>Chào</h2><p onclick=\"x()\">Nội dung <a href=\"javascript:alert(1)\">xấu</a></p>"
                + "<script>alert(1)</script><img src=x onerror=alert(1)>");
        body.put("published", true);
        send(put("/api/v1/admin/pages/trang-thu"), adminToken, body).andExpect(status().isOk());

        JsonNode page = data(send(get("/api/v1/pages/trang-thu"), null, null).andExpect(status().isOk()));
        assertThat(page.get("title").asString()).isEqualTo("Trang thử");
        assertThat(page.get("body").asString()).isEqualTo("<h2>Chào</h2><p>Nội dung xấu</p>");
        assertThat(data(send(get("/api/v1/pages"), null, null)).toString()).contains("trang-thu").doesNotContain("Nội dung");

        body.put("published", false);
        send(put("/api/v1/admin/pages/trang-thu"), adminToken, body).andExpect(status().isOk());
        send(get("/api/v1/pages/trang-thu"), null, null).andExpect(status().isNotFound());
        assertThat(data(send(get("/api/v1/pages"), null, null)).toString()).doesNotContain("trang-thu");
        assertThat(data(send(get("/api/v1/admin/pages"), adminToken, null)).toString()).contains("trang-thu");
        assertThat(data(send(get("/api/v1/admin/pages/trang-thu"), adminToken, null)).get("body").asString()).contains("Chào");

        send(delete("/api/v1/admin/pages/trang-thu"), adminToken, null).andExpect(status().isOk());
        send(get("/api/v1/admin/pages/trang-thu"), adminToken, null).andExpect(status().isNotFound());
    }

    @Test
    void pageRules_slugBodyAndRoles() throws Exception {
        Map<String, Object> body = Map.of("title", "Trang", "body", "<p>Nội dung</p>");
        send(put("/api/v1/admin/pages/trang"), null, body).andExpect(status().isUnauthorized());
        send(put("/api/v1/admin/pages/trang"), customerToken, body).andExpect(status().isForbidden());
        send(put("/api/v1/admin/pages/trang"), staffToken, body).andExpect(status().isForbidden());
        send(put("/api/v1/admin/pages/Trang_Hoa"), adminToken, body).andExpect(status().isBadRequest());
        send(put("/api/v1/admin/pages/trang"), adminToken, Map.of("title", "", "body", "<p>x</p>")).andExpect(status().isBadRequest());
        send(put("/api/v1/admin/pages/trang"), adminToken, Map.of("title", "T", "body", "<script>alert(1)</script>"))
                .andExpect(status().isBadRequest());
        send(put("/api/v1/admin/pages/trang"), adminToken, Map.of("title", "T", "body", "<div> </div>")).andExpect(status().isBadRequest());
        send(get("/api/v1/pages/khong-co"), null, null).andExpect(status().isNotFound());
    }

    @Test
    void seoFilesPointAtThePublicSite() throws Exception {
        String robots = send(get("/api/v1/seo/robots.txt"), null, null).andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        assertThat(robots).contains("Disallow: /admin/").contains("Disallow: /api/").contains("Sitemap: http").contains("/sitemap.xml");

        send(put("/api/v1/admin/pages/trang-seo"), adminToken, Map.of("title", "Trang SEO", "body", "<p>Nội dung</p>"))
                .andExpect(status().isOk());
        String sitemap = send(get("/api/v1/seo/sitemap.xml"), null, null).andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        assertThat(sitemap).startsWith("<?xml").contains("<urlset").contains("customer/products.html")
                .contains("customer/page.html?slug=trang-seo");
    }

    private void banner(String title, int order, boolean active, LocalDateTime from, LocalDateTime to) throws Exception {
        send(post("/api/v1/admin/banners"), adminToken, bannerBody(title, order, active, from, to)).andExpect(status().isCreated());
    }

    private static Map<String, Object> bannerBody(String title, int order, boolean active, LocalDateTime from, LocalDateTime to) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("title", title);
        body.put("subtitle", "Mô tả ngắn");
        body.put("imageUrl", "https://cdn.example.com/banner.jpg");
        body.put("linkUrl", "customer/products.html");
        body.put("displayOrder", order);
        body.put("active", active);
        body.put("startsAt", from == null ? null : from.toString());
        body.put("endsAt", to == null ? null : to.toString());
        return body;
    }

    private static Map<String, Object> withField(Map<String, Object> source, String field, Object value) {
        Map<String, Object> copy = new LinkedHashMap<>(source);
        copy.put(field, value);
        return copy;
    }

    private JsonNode register(String username) throws Exception {
        JsonNode data = data(send(post("/api/v1/auth/register"), null, Map.of(
                "email", username + "@example.com", "username", username, "password", PASSWORD,
                "fullname", "Người Dùng Nội Dung")).andExpect(status().isCreated()));
        registeredUserIds.add(data.get("user").get("id").asLong());
        return data;
    }

    private String registerWithRole(String username, String role) throws Exception {
        Long id = register(username).get("user").get("id").asLong();
        userRoleRepository.deleteAll(userRoleRepository.findAllByIdUserId(id));
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
