package com.example.Tech.controller.product;

import com.example.Tech.entity.product.Category;
import com.example.Tech.entity.product.Product;
import com.example.Tech.entity.product.ProductImage;
import com.example.Tech.entity.product.ProductVariant;
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

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Deleting a category with its products (only when none has a sales / stock / review history) and a hidden
 * category taking its products off the shop. Over HTTP against the test database (rolled back).
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class CategoryDeleteApiIntegrationTest {

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
    private String customerToken;
    private Category watches;
    private Product watchA;
    private ProductVariant watchAVariant;
    private Product watchB;

    @BeforeEach
    void setUp() throws Exception {
        customerToken = register("catdel.khach").get("accessToken").asString();
        adminToken = registerWithRole("catdel.admin", "ADMIN");

        watches = new Category();
        watches.setName("Đồng Hồ Thời Trang ZZ");
        watches.setSlug("test-catdel-dong-ho");
        entityManager.persist(watches);
        watchA = product("Đồng hồ ZZ A", "test-catdel-a");
        watchAVariant = variant(watchA);
        watchB = product("Đồng hồ ZZ B", "test-catdel-b");
        variant(watchB);
        StoreFixtures.warehouse(entityManager, watchAVariant);
        entityManager.flush();
    }

    @AfterEach
    void revokeTokens() {
        registeredUserIds.forEach(refreshTokenService::revokeAll);
    }

    @Test
    void delete_withProducts_needsTheFlag_thenRemovesProductsVariantsImagesCartLinesAndTheCategory() throws Exception {
        // stock rows count as history: drop them so the category is deletable, keep a cart line to be cleaned
        send(post("/api/v1/cart/items"), customerToken, Map.of("variantId", watchAVariant.getId(), "quantity", 1))
                .andExpect(status().isOk());
        entityManager.createNativeQuery("delete from inventory where variant_id = :v")
                .setParameter("v", watchAVariant.getId()).executeUpdate();

        send(delete("/api/v1/categories/" + watches.getId()), adminToken, null)
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error.code").value("RESOURCE_IN_USE"))
                .andExpect(jsonPath("$.error.details.reason").value("PRODUCTS"))
                .andExpect(jsonPath("$.error.details.products").value("2"));
        assertThat(count("select count(*) from products where category_id = " + watches.getId())).isEqualTo(2);

        send(delete("/api/v1/categories/" + watches.getId()).param("withProducts", "true"), adminToken, null)
                .andExpect(status().isNoContent());

        assertThat(count("select count(*) from categories where category_id = " + watches.getId())).isZero();
        assertThat(count("select count(*) from products where product_id in (" + watchA.getId() + ", " + watchB.getId() + ")")).isZero();
        assertThat(count("select count(*) from product_variants where variant_id = " + watchAVariant.getId())).isZero();
        assertThat(count("select count(*) from product_images where product_id = " + watchA.getId())).isZero();
        assertThat(count("select count(*) from cart_items where variant_id = " + watchAVariant.getId())).isZero();
    }

    @Test
    void delete_withProducts_whenOneHasStockOrdersOrReviews_isRefused_andNothingIsDeleted() throws Exception {
        send(delete("/api/v1/categories/" + watches.getId()).param("withProducts", "true"), adminToken, null)
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error.details.reason").value("HISTORY"))
                .andExpect(jsonPath("$.error.message").value(
                        "1 sản phẩm của danh mục đã có đơn hàng, tồn kho hoặc đánh giá nên không xoá được; hãy ẩn danh mục"));
        assertThat(count("select count(*) from products where category_id = " + watches.getId())).isEqualTo(2);
    }

    @Test
    void customers_cannotDeleteCategories() throws Exception {
        send(delete("/api/v1/categories/" + watches.getId()).param("withProducts", "true"), customerToken, null)
                .andExpect(status().isForbidden());
    }

    @Test
    void hiddenCategory_takesItsProductsOffTheShop_butTheAdminListStillShowsThem() throws Exception {
        send(get("/api/v1/products").param("isActive", "true").param("keyword", "test-catdel"), null, null)
                .andExpect(jsonPath("$.data.totalElements").value(2));

        Map<String, Object> body = new HashMap<>();
        body.put("name", watches.getName());
        body.put("slug", watches.getSlug());
        body.put("isActive", false);
        send(put("/api/v1/categories/" + watches.getId()), adminToken, body).andExpect(status().isOk());

        send(get("/api/v1/products").param("isActive", "true").param("keyword", "test-catdel"), null, null)
                .andExpect(jsonPath("$.data.totalElements").value(0));
        send(get("/api/v1/products").param("keyword", "test-catdel"), null, null)
                .andExpect(jsonPath("$.data.totalElements").value(2));
        send(post("/api/v1/cart/items"), customerToken, Map.of("variantId", watchAVariant.getId(), "quantity", 1))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error.code").value("PRODUCT_NOT_AVAILABLE"));
    }

    private Product product(String name, String slug) {
        Product product = new Product();
        product.setName(name);
        product.setSlug(slug);
        product.setCategory(watches);
        product.setBasePrice(new BigDecimal("2000000"));
        entityManager.persist(product);
        ProductImage image = new ProductImage();
        image.setProduct(product);
        image.setImageUrl("https://cdn.example/" + slug + ".jpg");
        image.setPrimary(true);
        entityManager.persist(image);
        return product;
    }

    private ProductVariant variant(Product product) {
        ProductVariant variant = new ProductVariant();
        variant.setProduct(product);
        variant.setVariantName("Mặc định");
        variant.setPrice(new BigDecimal("2000000"));
        entityManager.persist(variant);
        return variant;
    }

    private long count(String sql) {
        entityManager.flush();
        return ((Number) entityManager.createNativeQuery(sql).getSingleResult()).longValue();
    }

    private JsonNode register(String username) throws Exception {
        JsonNode data = data(send(post("/api/v1/auth/register"), null, Map.of(
                "email", username + "@example.com", "username", username, "password", PASSWORD,
                "fullname", "Người Thử Danh Mục")).andExpect(status().isCreated()));
        registeredUserIds.add(data.get("user").get("id").asLong());
        return data;
    }

    private String registerWithRole(String username, String role) throws Exception {
        Long id = register(username).get("user").get("id").asLong();
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
