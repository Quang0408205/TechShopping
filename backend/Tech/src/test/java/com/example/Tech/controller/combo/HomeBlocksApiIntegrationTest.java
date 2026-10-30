package com.example.Tech.controller.combo;

import com.example.Tech.entity.product.Category;
import com.example.Tech.entity.product.Product;
import com.example.Tech.entity.product.ProductVariant;
import com.example.Tech.entity.user.UserRole;
import com.example.Tech.repository.product.CategoryRepository;
import com.example.Tech.repository.product.ProductRepository;
import com.example.Tech.repository.product.ProductVariantRepository;
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
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** The public flash sale and featured combos of the home page. */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class HomeBlocksApiIntegrationTest {

    private static final String PASSWORD = "Matkhau@123";

    @Autowired private MockMvc mockMvc;
    @Autowired private JsonMapper jsonMapper;
    @Autowired private RefreshTokenService refreshTokenService;
    @Autowired private CategoryRepository categoryRepository;
    @Autowired private ProductRepository productRepository;
    @Autowired private ProductVariantRepository variantRepository;
    @Autowired private UserRepository userRepository;
    @Autowired private RoleRepository roleRepository;
    @Autowired private UserRoleRepository userRoleRepository;
    @Autowired private EntityManager entityManager;

    private final List<Long> registeredUserIds = new ArrayList<>();

    private String admin;
    private Category category;

    @BeforeEach
    void setUp() throws Exception {
        admin = registerWithRole("home.admin", "ADMIN");
        category = new Category();
        category.setName("Danh mục trang chủ");
        category.setSlug("test-home-blocks-category");
        category = categoryRepository.save(category);
    }

    @AfterEach
    void revokeTokens() {
        registeredUserIds.forEach(refreshTokenService::revokeAll);
    }

    @Test
    void flashSale_isPublic_deepestFirst_skipsSoldOut_countsDownToTheSoonestEnd() throws Exception {
        Product phone = stocked("Điện thoại Flash", "test-flash-phone", "15000000");
        Product cover = stocked("Ốp Flash", "test-flash-cover", "200000");
        Product soldOut = product("Hết hàng Flash", "test-flash-soldout", "1000000");
        variant(soldOut, "Mặc định", "1000000");
        LocalDateTime soon = LocalDateTime.now().plusDays(1).truncatedTo(ChronoUnit.SECONDS);
        LocalDateTime later = LocalDateTime.now().plusDays(3).truncatedTo(ChronoUnit.SECONDS);
        createPromotion("Flash 10", 10, later, List.of(Map.of("productId", phone.getId()), Map.of("productId", soldOut.getId())));
        createPromotion("Flash 40", 40, soon, List.of(Map.of("productId", cover.getId())));

        JsonNode data = data(send(get("/api/v1/promotions/flash-sale"), null, null).andExpect(status().isOk()));
        List<Long> ids = new ArrayList<>();
        data.get("items").forEach(item -> ids.add(item.get("id").asLong()));
        assertThat(ids).contains(phone.getId(), cover.getId()).doesNotContain(soldOut.getId());
        assertThat(ids.indexOf(cover.getId())).isLessThan(ids.indexOf(phone.getId()));
        assertThat(ids.size()).isLessThanOrEqualTo(8);
        JsonNode coverNode = null;
        for (JsonNode item : data.get("items")) {
            if (item.get("id").asLong() == cover.getId()) {
                coverNode = item;
            }
        }
        assertThat(coverNode.get("effectivePrice").decimalValue()).isEqualByComparingTo("120000");
        assertThat(coverNode.get("activePromotionName").asString()).isEqualTo("Flash 40");
        assertThat(LocalDateTime.parse(data.get("endsAt").asString())).isBeforeOrEqualTo(soon);
    }

    @Test
    void flashSale_ignoresPromotionsThatAreNotRunning() throws Exception {
        Product tablet = stocked("Máy tính bảng Flash", "test-flash-tablet", "9000000");
        // starts tomorrow: not running yet
        Map<String, Object> future = promotionBody("Chưa bắt đầu", 20, LocalDateTime.now().plusDays(5), List.of(Map.of("productId", tablet.getId())));
        future.put("startDate", LocalDateTime.now().plusDays(2).truncatedTo(ChronoUnit.SECONDS).toString());
        send(post("/api/v1/admin/promotions"), admin, future).andExpect(status().isCreated());
        // running but switched off
        Product watch = stocked("Đồng hồ Flash", "test-flash-watch", "3000000");
        Map<String, Object> off = promotionBody("Đã tắt", 20, LocalDateTime.now().plusDays(5), List.of(Map.of("productId", watch.getId())));
        off.put("active", false);
        send(post("/api/v1/admin/promotions"), admin, off).andExpect(status().isCreated());

        String body = send(get("/api/v1/promotions/flash-sale"), null, null).andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        assertThat(body).doesNotContain("\"id\":" + tablet.getId() + ",").doesNotContain("\"id\":" + watch.getId() + ",");
    }

    @Test
    void featuredCombos_arePublic_priceTheBasket_andSkipAMainProductThatIsSoldOut() throws Exception {
        Product laptop = stocked("Laptop Combo Home", "test-home-laptop", "20000000");
        Product cover = stocked("Ốp Combo Home", "test-home-cover", "200000");
        Product ghost = product("Máy hết hàng", "test-home-ghost", "5000000");
        variant(ghost, "Mặc định", "5000000");
        Long good = data(send(post("/api/v1/admin/combos"), admin, comboBody("Combo trang chủ", laptop, cover))
                .andExpect(status().isCreated())).get("id").asLong();
        Long hidden = data(send(post("/api/v1/admin/combos"), admin, comboBody("Combo hết hàng", ghost, cover))
                .andExpect(status().isCreated())).get("id").asLong();

        JsonNode list = data(send(get("/api/v1/combos/featured"), null, null).andExpect(status().isOk()));
        assertThat(list.size()).isLessThanOrEqualTo(3);
        JsonNode mine = null;
        for (JsonNode combo : list) {
            assertThat(combo.get("id").asLong()).isNotEqualTo(hidden);
            if (combo.get("id").asLong() == good) {
                mine = combo;
            }
        }
        assertThat(mine).isNotNull();
        assertThat(mine.get("main").get("productId").asLong()).isEqualTo(laptop.getId());
        assertThat(mine.get("items").size()).isEqualTo(1);
        assertThat(mine.get("originalTotal").decimalValue()).isEqualByComparingTo("20200000");
        assertThat(mine.get("comboTotal").decimalValue()).isEqualByComparingTo("20100000");
        assertThat(mine.get("saved").decimalValue()).isEqualByComparingTo("100000");
    }

    private Map<String, Object> comboBody(String name, Product main, Product addOn) {
        Map<String, Object> body = new HashMap<>();
        body.put("name", name);
        body.put("mainProductId", main.getId());
        body.put("startsAt", LocalDateTime.now().minusHours(1).truncatedTo(ChronoUnit.SECONDS).toString());
        body.put("endsAt", LocalDateTime.now().plusDays(1).truncatedTo(ChronoUnit.SECONDS).toString());
        body.put("items", List.of(Map.of("productId", addOn.getId(), "discountType", "PERCENT", "discountValue", 50)));
        return body;
    }

    private void createPromotion(String name, int percent, LocalDateTime ends, List<Map<String, Object>> products) throws Exception {
        send(post("/api/v1/admin/promotions"), admin, promotionBody(name, percent, ends, products)).andExpect(status().isCreated());
    }

    private static Map<String, Object> promotionBody(String name, int percent, LocalDateTime ends, List<Map<String, Object>> products) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("name", name);
        body.put("discountType", "PERCENTAGE");
        body.put("discountValue", percent);
        body.put("startDate", LocalDateTime.now().minusHours(1).truncatedTo(ChronoUnit.SECONDS).toString());
        body.put("endDate", ends.toString());
        body.put("active", true);
        body.put("products", products);
        return body;
    }

    private Product stocked(String name, String slug, String price) {
        Product product = product(name, slug, price);
        StoreFixtures.warehouse(entityManager, variant(product, "Mặc định", price));
        return product;
    }

    private Product product(String name, String slug, String price) {
        Product product = new Product();
        product.setName(name);
        product.setSlug(slug);
        product.setCategory(category);
        product.setBasePrice(new BigDecimal(price));
        return productRepository.save(product);
    }

    private ProductVariant variant(Product product, String name, String price) {
        ProductVariant variant = new ProductVariant();
        variant.setProduct(product);
        variant.setVariantName(name);
        variant.setPrice(new BigDecimal(price));
        return variantRepository.save(variant);
    }

    private String registerWithRole(String username, String role) throws Exception {
        JsonNode registered = data(send(post("/api/v1/auth/register"), null, Map.of(
                "email", username + "@example.com", "username", username, "password", PASSWORD,
                "fullname", "Người Thử Trang Chủ")).andExpect(status().isCreated()));
        Long id = registered.get("user").get("id").asLong();
        registeredUserIds.add(id);
        userRoleRepository.save(new UserRole(userRepository.getReferenceById(id), roleRepository.findByName(role).orElseThrow()));
        entityManager.flush();
        return data(send(post("/api/v1/auth/login"), null, Map.of("identifier", username, "password", PASSWORD))
                .andExpect(status().isOk())).get("accessToken").asString();
    }

    private ResultActions send(MockHttpServletRequestBuilder builder, String accessToken, Object body) throws Exception {
        if (accessToken != null) {
            builder.header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken);
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
