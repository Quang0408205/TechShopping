package com.example.Tech.controller.promotion;

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
import java.time.Clock;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * /api/v1/admin/promotions over HTTP against the test database (rolled back), with real ADMIN / STAFF /
 * customer accounts. Also checks that the cart and checkout charge the promotion price, and that a placed
 * order keeps it after the promotion is deleted. Promotion names start with "ZZ KM" so list assertions only
 * see this test's rows.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class AdminPromotionApiIntegrationTest {

    private static final String PASSWORD = "Matkhau@123";
    private static final String URL = "/api/v1/admin/promotions";

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
    private ProductVariantRepository variantRepository;

    @Autowired
    private EntityManager entityManager;

    @Autowired
    private Clock clock;

    private final List<Long> registeredUserIds = new ArrayList<>();

    private LocalDateTime now;
    private String customerToken;
    private String staffToken;
    private String adminToken;
    private Product phone;
    private Product laptop;
    private Product headphone;
    private Product deleted;
    private ProductVariant phoneBlack;
    private ProductVariant laptopBase;

    @BeforeEach
    void setUp() throws Exception {
        now = LocalDateTime.now(clock).truncatedTo(ChronoUnit.SECONDS);
        customerToken = register("km.customer").get("accessToken").asString();
        staffToken = registerWithRole("km.staff", "STAFF");
        adminToken = registerWithRole("km.admin", "ADMIN");

        Category category = new Category();
        category.setName("Khuyến mãi test");
        category.setSlug("test-km-category");
        category = categoryRepository.save(category);
        phone = productRepository.save(product("Điện thoại KM", "test-km-phone", category));
        laptop = productRepository.save(product("Laptop KM", "test-km-laptop", category));
        headphone = productRepository.save(product("Tai nghe KM", "test-km-headphone", category));
        deleted = product("Sản phẩm đã xoá KM", "test-km-deleted", category);
        deleted.setDeletedAt(now.minusDays(1));
        deleted = productRepository.save(deleted);
        phoneBlack = variantRepository.save(variant(phone, "Đen", "1000000"));
        laptopBase = variantRepository.save(variant(laptop, "Bản thường", "10000000"));
        entityManager.flush();
    }

    @AfterEach
    void revokeTokens() {
        registeredUserIds.forEach(refreshTokenService::revokeAll);
    }

    @Test
    void access_adminOnly() throws Exception {
        send(get(URL), null, null).andExpect(status().isUnauthorized());
        send(get(URL), customerToken, null)
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error.code").value("ACCESS_DENIED"));
        send(get(URL), staffToken, null)
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error.code").value("ACCESS_DENIED"));
        send(post(URL), staffToken, body("ZZ KM staff", now.minusHours(1), now.plusDays(1), true, List.of(item(phone))))
                .andExpect(status().isForbidden());
        send(get(URL), adminToken, null).andExpect(status().isOk());
    }

    @Test
    void create_returnsEffectiveDiscounts_andTheCartAndCheckoutChargeThePromotionPrice() throws Exception {
        Map<String, Object> request = body("  ZZ KM Flash Sale  ", now.minusHours(1), now.plusDays(1), true,
                List.of(item(phone), override(laptop, "FIXED_AMOUNT", "500000")));
        request.put("description", "  Giảm sốc cuối tuần ");

        JsonNode created = data(send(post(URL), adminToken, request).andExpect(status().isCreated()));

        long id = created.get("id").asLong();
        assertThat(created.get("name").asString()).isEqualTo("ZZ KM Flash Sale");
        assertThat(created.get("description").asString()).isEqualTo("Giảm sốc cuối tuần");
        assertThat(created.get("status").asString()).isEqualTo("RUNNING");
        assertThat(created.get("active").asBoolean()).isTrue();
        assertThat(created.get("createdByName").asString()).isEqualTo("Người Dùng Khuyến Mãi");
        JsonNode products = created.get("products");
        assertThat(products).hasSize(2);
        assertThat(products.get(0).get("productId").asLong()).isEqualTo(phone.getId());
        assertThat(products.get(0).get("discountType").asString()).isEqualTo("PERCENTAGE");
        assertThat(products.get(0).get("discountValue").decimalValue()).isEqualByComparingTo("10");
        assertThat(products.get(0).get("override").asBoolean()).isFalse();
        assertThat(products.get(0).get("productAvailable").asBoolean()).isTrue();
        assertThat(products.get(1).get("productId").asLong()).isEqualTo(laptop.getId());
        assertThat(products.get(1).get("discountType").asString()).isEqualTo("FIXED_AMOUNT");
        assertThat(products.get(1).get("discountValue").decimalValue()).isEqualByComparingTo("500000");
        assertThat(products.get(1).get("override").asBoolean()).isTrue();

        send(get(URL + "/" + id), adminToken, null)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.products.length()").value(2));

        // public catalogue: effectivePrice + promotion name, discountPrice stays the raw admin value (null here)
        JsonNode phoneView = data(send(get("/api/v1/products/" + phone.getId()), null, null).andExpect(status().isOk()));
        assertThat(phoneView.get("effectivePrice").decimalValue()).isEqualByComparingTo("900000");
        assertThat(phoneView.get("activePromotionName").asString()).isEqualTo("ZZ KM Flash Sale");
        assertThat(phoneView.get("discountPrice").isNull()).isTrue();
        JsonNode listed = data(send(get("/api/v1/products").param("keyword", "Laptop KM"), null, null).andExpect(status().isOk()));
        assertThat(listed.get("content").get(0).get("effectivePrice").decimalValue()).isEqualByComparingTo("500000");
        JsonNode variants = data(send(get("/api/v1/products/" + phone.getId() + "/variants"), null, null)
                .andExpect(status().isOk()));
        assertThat(variants.get(0).get("effectivePrice").decimalValue()).isEqualByComparingTo("900000");
        assertThat(variants.get(0).get("activePromotionName").asString()).isEqualTo("ZZ KM Flash Sale");
        JsonNode headphoneView = data(send(get("/api/v1/products/" + headphone.getId()), null, null).andExpect(status().isOk()));
        assertThat(headphoneView.get("effectivePrice").decimalValue()).isEqualByComparingTo("1000000");
        assertThat(headphoneView.get("activePromotionName").isNull()).isTrue();

        // the customer's cart and checkout use the promotion price (10% off the phone, 500.000đ off the laptop)
        addToCart(phoneBlack, 1);
        addToCart(laptopBase, 1);
        JsonNode cart = data(send(get("/api/v1/cart"), customerToken, null).andExpect(status().isOk()));
        assertThat(cart.get("items").get(0).get("unitPrice").decimalValue()).isEqualByComparingTo("900000");
        assertThat(cart.get("items").get(0).get("originalPrice").decimalValue()).isEqualByComparingTo("1000000");
        assertThat(cart.get("items").get(1).get("unitPrice").decimalValue()).isEqualByComparingTo("9500000");
        assertThat(cart.get("subtotal").decimalValue()).isEqualByComparingTo("10400000");

        JsonNode order = data(send(post("/api/v1/orders"), customerToken, Map.of(
                "recipientName", "Nguyễn Văn Khuyến", "recipientPhone", "0901234567",
                "shippingAddress", "12 Nguyễn Trãi, Quận 5", "paymentMethod", "COD"))
                .andExpect(status().isCreated()));
        long orderId = order.get("id").asLong();
        assertThat(order.get("items").get(0).get("unitPrice").decimalValue()).isEqualByComparingTo("900000");
        assertThat(order.get("items").get(0).get("discountAmount").decimalValue()).isEqualByComparingTo("100000");

        // deleting the promotion: the placed order keeps its prices, new carts go back to the list price
        send(delete(URL + "/" + id), adminToken, null).andExpect(status().isNoContent());
        entityManager.flush();
        entityManager.clear();
        JsonNode kept = data(send(get("/api/v1/orders/" + orderId), customerToken, null).andExpect(status().isOk()));
        assertThat(kept.get("items").get(0).get("unitPrice").decimalValue()).isEqualByComparingTo("900000");
        assertThat(kept.get("total").decimalValue()).isEqualByComparingTo("10400000");
        addToCart(phoneBlack, 1);
        JsonNode cartAfter = data(send(get("/api/v1/cart"), customerToken, null).andExpect(status().isOk()));
        assertThat(cartAfter.get("items").get(0).get("unitPrice").decimalValue()).isEqualByComparingTo("1000000");
    }

    @Test
    void create_invalidData_isRefusedWithTheRightCode() throws Exception {
        expectError(body("ZZ KM ngày sai", now.plusDays(1), now.plusDays(1), true, List.of(item(phone))),
                400, "INVALID_PROMOTION_DATE_RANGE");
        Map<String, Object> over100 = body("ZZ KM 150%", now, now.plusDays(1), true, List.of(item(phone)));
        over100.put("discountValue", 150);
        expectError(over100, 400, "INVALID_PROMOTION_DISCOUNT");
        Map<String, Object> half = new HashMap<>(item(phone));
        half.put("discountType", "PERCENTAGE");
        expectError(body("ZZ KM nửa", now, now.plusDays(1), true, List.of(half)), 400, "INVALID_PROMOTION_DISCOUNT");
        expectError(body("ZZ KM override 120%", now, now.plusDays(1), true,
                List.of(override(phone, "PERCENTAGE", "120"))), 400, "INVALID_PROMOTION_DISCOUNT");
        expectError(body("ZZ KM trùng", now, now.plusDays(1), true, List.of(item(phone), item(phone))),
                400, "VALIDATION_ERROR");
        expectError(body("ZZ KM không có", now, now.plusDays(1), true, List.of(Map.of("productId", 999999999))),
                404, "PRODUCT_NOT_FOUND");
        expectError(body("ZZ KM đã xoá", now, now.plusDays(1), true, List.of(item(deleted))),
                404, "PRODUCT_NOT_FOUND");

        send(post(URL), adminToken, body(" ", now, now.plusDays(1), true, List.of()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.error.details.name").value("Vui lòng nhập tên chương trình"))
                .andExpect(jsonPath("$.error.details.products").value("Vui lòng chọn ít nhất một sản phẩm"));
        Map<String, Object> zero = body("ZZ KM 0", now, now.plusDays(1), true, List.of(item(phone)));
        zero.put("discountValue", 0);
        send(post(URL), adminToken, zero)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.details.discountValue").value("Mức giảm phải lớn hơn 0"));

        assertThat(search("keyword", "ZZ KM").get("totalElements").asLong()).isZero();
    }

    @Test
    void overlap_activePromotionsOnTheSameProductCannotOverlap() throws Exception {
        LocalDateTime start = now.minusHours(1);
        LocalDateTime end = now.plusDays(1);
        Map<String, Object> first = body("ZZ KM A", start, end, true, List.of(item(phone)));
        long a = data(send(post(URL), adminToken, first).andExpect(status().isCreated())).get("id").asLong();

        send(post(URL), adminToken, body("ZZ KM B", now, now.plusDays(2), true, List.of(item(laptop), item(phone))))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error.code").value("PROMOTION_PRODUCT_OVERLAP"))
                .andExpect(jsonPath("$.error.message", containsString("Điện thoại KM")));
        // nothing was saved: the laptop is still free
        data(send(post(URL), adminToken, body("ZZ KM laptop", now, now.plusDays(2), true, List.of(item(laptop))))
                .andExpect(status().isCreated()));

        // starting exactly when A ends is not an overlap (end time exclusive)
        data(send(post(URL), adminToken, body("ZZ KM C", end, end.plusDays(1), true, List.of(item(phone))))
                .andExpect(status().isCreated()));

        // a paused promotion may overlap; switching it on is checked again
        Map<String, Object> paused = body("ZZ KM D", now, now.plusHours(5), false, List.of(item(phone)));
        long d = data(send(post(URL), adminToken, paused).andExpect(status().isCreated())).get("id").asLong();
        paused.put("active", true);
        send(put(URL + "/" + d), adminToken, paused)
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error.code").value("PROMOTION_PRODUCT_OVERLAP"));

        // updating A over its own period does not conflict with itself
        first.put("name", "ZZ KM A sửa");
        send(put(URL + "/" + a), adminToken, first)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.name").value("ZZ KM A sửa"));
    }

    @Test
    void update_replacesFieldsAndTheProductList() throws Exception {
        long id = data(send(post(URL), adminToken, body("ZZ KM cũ", now.minusHours(1), now.plusDays(1), true,
                List.of(item(phone), override(laptop, "FIXED_AMOUNT", "1000000"))))
                .andExpect(status().isCreated())).get("id").asLong();

        Map<String, Object> changed = body("ZZ KM mới", now.plusDays(2), now.plusDays(3), true,
                List.of(override(laptop, "PERCENTAGE", "20"), item(headphone)));
        changed.put("discountType", "FIXED_AMOUNT");
        changed.put("discountValue", 300000);
        changed.put("maxDiscountAmount", 1500000);
        JsonNode updated = data(send(put(URL + "/" + id), adminToken, changed).andExpect(status().isOk()));

        assertThat(updated.get("name").asString()).isEqualTo("ZZ KM mới");
        assertThat(updated.get("status").asString()).isEqualTo("UPCOMING");
        assertThat(updated.get("discountType").asString()).isEqualTo("FIXED_AMOUNT");
        assertThat(updated.get("maxDiscountAmount").decimalValue()).isEqualByComparingTo("1500000");
        JsonNode products = updated.get("products");
        assertThat(products).hasSize(2);
        assertThat(products.get(0).get("productId").asLong()).isEqualTo(laptop.getId());
        assertThat(products.get(0).get("discountType").asString()).isEqualTo("PERCENTAGE");
        assertThat(products.get(0).get("discountValue").decimalValue()).isEqualByComparingTo("20");
        assertThat(products.get(0).get("override").asBoolean()).isTrue();
        assertThat(products.get(1).get("productId").asLong()).isEqualTo(headphone.getId());
        assertThat(products.get(1).get("discountType").asString()).isEqualTo("FIXED_AMOUNT");
        assertThat(products.get(1).get("override").asBoolean()).isFalse();

        send(put(URL + "/999999999"), adminToken, changed)
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error.code").value("PROMOTION_NOT_FOUND"));
    }

    @Test
    void search_byStatusKeywordAndDates() throws Exception {
        long running = create("ZZ KM Đang Chạy", now.minusHours(1), now.plusDays(1), true);
        long upcoming = create("ZZ KM Sắp Tới", now.plusDays(2), now.plusDays(3), true);
        long paused = create("ZZ KM Tạm Dừng", now.minusHours(1), now.plusDays(1), false);
        long ended = create("ZZ KM Đã Hết", now.minusDays(3), now.minusDays(2), true);

        assertIds(search("keyword", "ZZ KM"), ended, paused, upcoming, running);
        assertIds(search("keyword", "ZZ KM", "status", "RUNNING"), running);
        assertIds(search("keyword", "ZZ KM", "status", "UPCOMING"), upcoming);
        assertIds(search("keyword", "ZZ KM", "status", "PAUSED"), paused);
        assertIds(search("keyword", "ZZ KM", "status", "ENDED"), ended);
        assertIds(search("keyword", "zz km đang"), running);
        String day = now.toLocalDate().plusDays(2).toString();
        assertIds(search("keyword", "ZZ KM", "fromDate", day, "toDate", day), upcoming);
        assertThat(search("keyword", "ZZ KM").get("content").get(0).get("status").asString()).isEqualTo("ENDED");

        send(get(URL).param("fromDate", "2026-12-31").param("toDate", "2026-01-01"), adminToken, null)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("VALIDATION_ERROR"));
        send(get(URL).param("status", "LOST"), adminToken, null).andExpect(status().isBadRequest());
    }

    @Test
    void delete_unknown_isNotFound() throws Exception {
        send(delete(URL + "/999999999"), adminToken, null)
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error.code").value("PROMOTION_NOT_FOUND"))
                .andExpect(jsonPath("$.error.message").value("Không tìm thấy chương trình khuyến mãi id 999999999"));
    }

    private long create(String name, LocalDateTime start, LocalDateTime end, boolean active) throws Exception {
        return data(send(post(URL), adminToken, body(name, start, end, active, List.of(item(phone))))
                .andExpect(status().isCreated())).get("id").asLong();
    }

    private void addToCart(ProductVariant variant, int quantity) throws Exception {
        send(post("/api/v1/cart/items"), customerToken, Map.of("variantId", variant.getId(), "quantity", quantity))
                .andExpect(status().isOk());
    }

    private void expectError(Map<String, Object> request, int httpStatus, String code) throws Exception {
        send(post(URL), adminToken, request)
                .andExpect(status().is(httpStatus))
                .andExpect(jsonPath("$.error.code").value(code));
    }

    private JsonNode search(String... params) throws Exception {
        MockHttpServletRequestBuilder builder = get(URL);
        for (int i = 0; i < params.length; i += 2) {
            builder.param(params[i], params[i + 1]);
        }
        return data(send(builder, adminToken, null).andExpect(status().isOk()));
    }

    private static void assertIds(JsonNode page, long... expectedIds) {
        List<Long> ids = new ArrayList<>();
        page.get("content").forEach(row -> ids.add(row.get("id").asLong()));
        assertThat(ids).containsExactly(java.util.Arrays.stream(expectedIds).boxed().toArray(Long[]::new));
    }

    private static Map<String, Object> body(String name, LocalDateTime start, LocalDateTime end, boolean active,
                                            List<Map<String, Object>> products) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("name", name);
        body.put("discountType", "PERCENTAGE");
        body.put("discountValue", 10);
        body.put("startDate", start.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
        body.put("endDate", end.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
        body.put("active", active);
        body.put("products", products);
        return body;
    }

    private static Map<String, Object> item(Product product) {
        return Map.of("productId", product.getId());
    }

    private static Map<String, Object> override(Product product, String type, String value) {
        return Map.of("productId", product.getId(), "discountType", type, "discountValue", new BigDecimal(value));
    }

    private JsonNode register(String username) throws Exception {
        JsonNode data = data(send(post("/api/v1/auth/register"), null, Map.of(
                "email", username + "@example.com", "username", username, "password", PASSWORD,
                "fullname", "Người Dùng Khuyến Mãi"))
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

    private static Product product(String name, String slug, Category category) {
        Product product = new Product();
        product.setName(name);
        product.setSlug(slug);
        product.setCategory(category);
        product.setBasePrice(new BigDecimal("1000000"));
        return product;
    }

    private static ProductVariant variant(Product product, String name, String price) {
        ProductVariant variant = new ProductVariant();
        variant.setProduct(product);
        variant.setVariantName(name);
        variant.setPrice(new BigDecimal(price));
        return variant;
    }
}
