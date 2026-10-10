package com.example.Tech.controller.review;

import com.example.Tech.entity.product.Category;
import com.example.Tech.entity.product.Product;
import com.example.Tech.entity.user.UserRole;
import com.example.Tech.repository.product.CategoryRepository;
import com.example.Tech.repository.product.ProductRepository;
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

import java.math.BigDecimal;
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
 * /api/v1/admin/reviews over HTTP against the test database (rolled back): ADMIN only, filters, hide with a
 * reason / show again, and the product rating plus the public list following the change.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class AdminReviewApiIntegrationTest {

    private static final String PASSWORD = "Matkhau@123";
    private static final String URL = "/api/v1/admin/reviews";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JsonMapper jsonMapper;

    @Autowired
    private RefreshTokenService refreshTokenService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private UserRoleRepository userRoleRepository;

    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private EntityManager entityManager;

    private final List<Long> registeredUserIds = new ArrayList<>();

    private String adminToken;
    private String staffToken;
    private String tokenA;
    private String tokenB;
    private Product phone;
    private Product laptop;
    private long reviewA;
    private long reviewB;

    @BeforeEach
    void setUp() throws Exception {
        adminToken = registerWithRole("ar.admin", "Quản Trị Đánh Giá", "ADMIN");
        staffToken = registerWithRole("ar.staff", "Nhân Viên", "STAFF");
        tokenA = register("ar.khacha", "Khách Hàng A").get("accessToken").asString();
        tokenB = register("ar.khachb", "Khách Hàng B").get("accessToken").asString();

        Category category = new Category();
        category.setName("Kiểm duyệt test");
        category.setSlug("test-ar-category");
        category = categoryRepository.save(category);
        phone = productRepository.save(product("Điện thoại ZZAR", "test-ar-phone", category));
        laptop = productRepository.save(product("Laptop ZZAR", "test-ar-laptop", category));
        entityManager.flush();

        reviewA = write(tokenA, phone, 1, "Quảng cáo: mua hàng giá rẻ tại web khác zzspam");
        reviewB = write(tokenB, phone, 5, "Điện thoại rất tốt, pin khoẻ zzgood");
        write(tokenB, laptop, 4, "Laptop ổn trong tầm giá zzgood");
    }

    @AfterEach
    void revokeTokens() {
        registeredUserIds.forEach(refreshTokenService::revokeAll);
    }

    @Test
    void access_adminOnly() throws Exception {
        send(get(URL), null, null).andExpect(status().isUnauthorized());
        send(get(URL), tokenA, null).andExpect(status().isForbidden());
        send(get(URL), staffToken, null).andExpect(status().isForbidden());
        send(patch(URL + "/" + reviewA + "/visibility"), staffToken, visibility(true, "Spam"))
                .andExpect(status().isForbidden());
        send(get(URL), adminToken, null).andExpect(status().isOk());
    }

    @Test
    void filters() throws Exception {
        assertThat(ids("keyword", "zzar")).containsExactlyInAnyOrder(reviewA, reviewB, idOf(laptop));
        assertThat(ids("keyword", "zzspam")).containsExactly(reviewA);
        assertThat(ids("keyword", "khách hàng b")).hasSize(2);
        assertThat(ids("keyword", "ar.khacha@example")).containsExactly(reviewA);
        assertThat(ids("keyword", "zzar", "rating", "5")).containsExactly(reviewB);
        assertThat(ids("productId", String.valueOf(laptop.getId()))).containsExactly(idOf(laptop));
        assertThat(ids("keyword", "zzar", "hidden", "true")).isEmpty();

        JsonNode row = data(send(get(URL).param("keyword", "zzspam"), adminToken, null)).get("content").get(0);
        assertThat(row.get("authorEmail").asString()).isEqualTo("ar.khacha@example.com");
        assertThat(row.get("productName").asString()).isEqualTo("Điện thoại ZZAR");
        assertThat(row.get("verifiedPurchase").asBoolean()).isFalse();
    }

    @Test
    void hide_thenShow_theRatingAndThePublicListFollow() throws Exception {
        assertThat(productRepository.findById(phone.getId()).orElseThrow().getRating()).isEqualByComparingTo("3.00");

        send(patch(URL + "/" + reviewA + "/visibility"), adminToken, visibility(true, "  "))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.details.reason").exists());
        send(patch(URL + "/-1/visibility"), adminToken, visibility(true, "Spam"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error.code").value("REVIEW_NOT_FOUND"));

        JsonNode hidden = data(send(patch(URL + "/" + reviewA + "/visibility"), adminToken,
                visibility(true, "Nội dung quảng cáo")).andExpect(status().isOk()));
        assertThat(hidden.get("hidden").asBoolean()).isTrue();
        assertThat(hidden.get("hiddenReason").asString()).isEqualTo("Nội dung quảng cáo");
        assertThat(hidden.get("hiddenByName").asString()).isEqualTo("Quản Trị Đánh Giá");
        assertThat(hidden.get("edited").asBoolean()).isFalse();
        Product afterHide = productRepository.findById(phone.getId()).orElseThrow();
        assertThat(afterHide.getRating()).isEqualByComparingTo("5.00");
        assertThat(afterHide.getTotalReviews()).isEqualTo(1);
        assertThat(publicIds(phone)).containsExactly(reviewB);
        assertThat(ids("keyword", "zzar", "hidden", "true")).containsExactly(reviewA);
        assertThat(data(send(get("/api/v1/reviews/mine"), tokenA, null)).get(0).get("hiddenReason").asString())
                .isEqualTo("Nội dung quảng cáo");

        JsonNode shown = data(send(patch(URL + "/" + reviewA + "/visibility"), adminToken, visibility(false, null))
                .andExpect(status().isOk()));
        assertThat(shown.get("hidden").asBoolean()).isFalse();
        assertThat(shown.get("hiddenReason").isNull()).isTrue();
        assertThat(productRepository.findById(phone.getId()).orElseThrow().getTotalReviews()).isEqualTo(2);
        assertThat(publicIds(phone)).containsExactlyInAnyOrder(reviewA, reviewB);
    }

    private long idOf(Product product) throws Exception {
        return data(send(get(URL).param("productId", String.valueOf(product.getId())), adminToken, null))
                .get("content").get(0).get("id").asLong();
    }

    private List<Long> ids(String... params) throws Exception {
        MockHttpServletRequestBuilder builder = get(URL);
        for (int i = 0; i < params.length; i += 2) {
            builder.param(params[i], params[i + 1]);
        }
        List<Long> ids = new ArrayList<>();
        data(send(builder, adminToken, null).andExpect(status().isOk())).get("content")
                .forEach(row -> ids.add(row.get("id").asLong()));
        return ids;
    }

    private List<Long> publicIds(Product product) throws Exception {
        List<Long> ids = new ArrayList<>();
        data(send(get("/api/v1/products/" + product.getId() + "/reviews"), null, null)).get("content")
                .forEach(row -> ids.add(row.get("id").asLong()));
        return ids;
    }

    private long write(String token, Product product, int rating, String comment) throws Exception {
        return data(send(post("/api/v1/reviews"), token,
                Map.of("productId", product.getId(), "rating", rating, "comment", comment))
                .andExpect(status().isCreated())).get("id").asLong();
    }

    private static Map<String, Object> visibility(boolean hidden, String reason) {
        Map<String, Object> body = new HashMap<>();
        body.put("hidden", hidden);
        body.put("reason", reason);
        return body;
    }

    private static Product product(String name, String slug, Category category) {
        Product product = new Product();
        product.setName(name);
        product.setSlug(slug);
        product.setCategory(category);
        product.setBasePrice(new BigDecimal("1000000"));
        return product;
    }

    private JsonNode register(String username, String fullname) throws Exception {
        JsonNode data = data(send(post("/api/v1/auth/register"), null, Map.of(
                "email", username + "@example.com", "username", username, "password", PASSWORD,
                "fullname", fullname))
                .andExpect(status().isCreated()));
        registeredUserIds.add(data.get("user").get("id").asLong());
        return data;
    }

    /** Registers, adds the role in the DB, then logs in again so the access token carries it. */
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
