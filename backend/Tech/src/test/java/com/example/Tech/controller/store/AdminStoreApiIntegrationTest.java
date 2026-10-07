package com.example.Tech.controller.store;

import com.example.Tech.entity.inventory.Inventory;
import com.example.Tech.entity.product.Category;
import com.example.Tech.entity.product.Product;
import com.example.Tech.entity.product.ProductVariant;
import com.example.Tech.entity.user.UserRole;
import com.example.Tech.repository.inventory.InventoryRepository;
import com.example.Tech.repository.product.CategoryRepository;
import com.example.Tech.repository.product.ProductRepository;
import com.example.Tech.repository.product.ProductVariantRepository;
import com.example.Tech.repository.store.StoreRepository;
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
import java.util.LinkedHashMap;
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
 * /api/v1/admin/stores and the public /api/v1/stores over HTTP against the test database (rolled back),
 * with real ADMIN / STAFF / customer accounts. Store names start with "ZZ CN" so list assertions only see
 * this test's rows.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class AdminStoreApiIntegrationTest {

    private static final String PASSWORD = "Matkhau@123";
    private static final String URL = "/api/v1/admin/stores";
    private static final String PUBLIC_URL = "/api/v1/stores";

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
    private StoreRepository storeRepository;

    @Autowired
    private InventoryRepository inventoryRepository;

    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private ProductVariantRepository variantRepository;

    @Autowired
    private EntityManager entityManager;

    private final List<Long> registeredUserIds = new ArrayList<>();

    private String customerToken;
    private String staffToken;
    private String adminToken;

    @BeforeEach
    void setUp() throws Exception {
        customerToken = register("cn.customer").get("accessToken").asString();
        staffToken = registerWithRole("cn.staff", "STAFF");
        adminToken = registerWithRole("cn.admin", "ADMIN");
    }

    @AfterEach
    void revokeTokens() {
        registeredUserIds.forEach(refreshTokenService::revokeAll);
    }

    @Test
    void access_adminOnly_butTheOpenListIsPublic() throws Exception {
        send(get(URL), null, null).andExpect(status().isUnauthorized());
        send(get(URL), customerToken, null)
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error.code").value("ACCESS_DENIED"));
        send(get(URL), staffToken, null).andExpect(status().isForbidden());
        send(post(URL), staffToken, body("ZZ CN Staff", "Quận 1", "Hồ Chí Minh")).andExpect(status().isForbidden());
        send(get(URL), adminToken, null).andExpect(status().isOk());

        send(get(PUBLIC_URL), null, null).andExpect(status().isOk());
    }

    @Test
    void create_update_close_andThePublicListFollows() throws Exception {
        Map<String, Object> request = body("  ZZ CN Quận 1  ", "Quận 1", "Hồ Chí Minh");
        request.put("phone", " 028 3812 3456 ");
        request.put("email", "");
        request.put("latitude", new BigDecimal("10.7769"));
        request.put("longitude", new BigDecimal("106.7009"));

        JsonNode created = data(send(post(URL), adminToken, request).andExpect(status().isCreated()));

        int id = created.get("id").asInt();
        assertThat(created.get("name").asString()).isEqualTo("ZZ CN Quận 1");
        assertThat(created.get("phone").asString()).isEqualTo("028 3812 3456");
        assertThat(created.get("email").isNull()).isTrue();
        assertThat(created.get("active").asBoolean()).isTrue();
        assertThat(created.get("latitude").decimalValue()).isEqualByComparingTo("10.7769");
        assertThat(publicNames()).contains("ZZ CN Quận 1");

        Map<String, Object> closed = body("ZZ CN Quận 1 (cũ)", "Quận 1", "Hồ Chí Minh");
        closed.put("active", false);
        JsonNode updated = data(send(put(URL + "/" + id), adminToken, closed).andExpect(status().isOk()));

        assertThat(updated.get("name").asString()).isEqualTo("ZZ CN Quận 1 (cũ)");
        assertThat(updated.get("active").asBoolean()).isFalse();
        assertThat(updated.get("latitude").isNull()).isTrue();
        assertThat(publicNames()).doesNotContain("ZZ CN Quận 1", "ZZ CN Quận 1 (cũ)");

        JsonNode fetched = data(send(get(URL + "/" + id), adminToken, null).andExpect(status().isOk()));
        assertThat(fetched.get("active").asBoolean()).isFalse();
    }

    @Test
    void invalidRequests_areRejectedWithFieldDetails() throws Exception {
        Map<String, Object> noCity = body("ZZ CN Thiếu", "Quận 1", " ");
        send(post(URL), adminToken, noCity)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.error.details.city").exists());

        Map<String, Object> onlyLatitude = body("ZZ CN Toạ độ", "Quận 1", "Hồ Chí Minh");
        onlyLatitude.put("latitude", 10.5);
        send(post(URL), adminToken, onlyLatitude)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.details.longitude").exists());

        Map<String, Object> badEmailAndPhone = body("ZZ CN Email", "Quận 1", "Hồ Chí Minh");
        badEmailAndPhone.put("email", "khong-phai-email");
        badEmailAndPhone.put("phone", "abc");
        send(post(URL), adminToken, badEmailAndPhone)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.details.email").exists())
                .andExpect(jsonPath("$.error.details.phone").exists());

        send(put(URL + "/-1"), adminToken, body("ZZ CN Không có", "Quận 1", "Hồ Chí Minh"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error.code").value("STORE_NOT_FOUND"));
    }

    @Test
    void search_byKeywordAndActive() throws Exception {
        int district1 = create("ZZ CN Bến Thành", "Quận 1", "Hồ Chí Minh", true);
        int hanoi = create("ZZ CN Hoàn Kiếm", "Hoàn Kiếm", "Hà Nội", true);
        int closed = create("ZZ CN Đã đóng", "Quận 1", "Hồ Chí Minh", false);

        assertThat(searchIds("keyword", "zz cn")).containsExactly(district1, closed, hanoi);
        assertThat(searchIds("keyword", "hà nội")).containsExactly(hanoi);
        assertThat(searchIds("keyword", "ZZ CN", "active", "false")).containsExactly(closed);
        assertThat(searchIds("keyword", "zz cn", "active", "true")).containsExactly(district1, hanoi);
    }

    @Test
    void delete_unusedStore_isRemoved_butAStoreWithStockIsInUse() throws Exception {
        int unused = create("ZZ CN Xoá được", "Quận 5", "Hồ Chí Minh", true);
        send(delete(URL + "/" + unused), adminToken, null).andExpect(status().isNoContent());
        send(get(URL + "/" + unused), adminToken, null)
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error.code").value("STORE_NOT_FOUND"));

        int withStock = create("ZZ CN Có hàng", "Quận 7", "Hồ Chí Minh", true);
        inventoryRepository.save(new Inventory(storeRepository.getReferenceById(withStock), variant(), 3));
        entityManager.flush();

        send(delete(URL + "/" + withStock), adminToken, null)
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error.code").value("STORE_IN_USE"));
        assertThat(storeRepository.findById(withStock)).isPresent();
    }

    private int create(String name, String district, String city, boolean active) throws Exception {
        Map<String, Object> request = body(name, district, city);
        request.put("active", active);
        return data(send(post(URL), adminToken, request).andExpect(status().isCreated())).get("id").asInt();
    }

    private List<Integer> searchIds(String... params) throws Exception {
        MockHttpServletRequestBuilder builder = get(URL);
        for (int i = 0; i < params.length; i += 2) {
            builder.param(params[i], params[i + 1]);
        }
        List<Integer> ids = new ArrayList<>();
        data(send(builder, adminToken, null).andExpect(status().isOk())).get("content")
                .forEach(row -> ids.add(row.get("id").asInt()));
        return ids;
    }

    private List<String> publicNames() throws Exception {
        List<String> names = new ArrayList<>();
        data(send(get(PUBLIC_URL), null, null).andExpect(status().isOk()))
                .forEach(row -> names.add(row.get("name").asString()));
        return names;
    }

    private ProductVariant variant() {
        Category category = new Category();
        category.setName("Chi nhánh test");
        category.setSlug("test-cn-category");
        category = categoryRepository.save(category);
        Product product = new Product();
        product.setName("Sản phẩm CN");
        product.setSlug("test-cn-product");
        product.setCategory(category);
        product.setBasePrice(new BigDecimal("1000000"));
        product = productRepository.save(product);
        ProductVariant variant = new ProductVariant();
        variant.setProduct(product);
        variant.setVariantName("Mặc định");
        variant.setPrice(new BigDecimal("1000000"));
        return variantRepository.save(variant);
    }

    private static Map<String, Object> body(String name, String district, String city) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("name", name);
        body.put("address", "1 Đường Test");
        body.put("district", district);
        body.put("city", city);
        return body;
    }

    private JsonNode register(String username) throws Exception {
        JsonNode data = data(send(post("/api/v1/auth/register"), null, Map.of(
                "email", username + "@example.com", "username", username, "password", PASSWORD,
                "fullname", "Người Dùng Chi Nhánh"))
                .andExpect(status().isCreated()));
        registeredUserIds.add(data.get("user").get("id").asLong());
        return data;
    }

    /** Registers, adds the role in the DB, then logs in again so the access token carries it. */
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
