package com.example.Tech.controller.voucher;

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
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Admin voucher API + customer preview + checkout with a voucher + use given back on cancel. */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class VoucherApiIntegrationTest {

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
    private String staff;
    private String customer;
    private Category phones;
    private Category other;
    private ProductVariant phone;
    private ProductVariant cover;

    @BeforeEach
    void setUp() throws Exception {
        admin = registerWithRole("voucher.admin", "ADMIN");
        staff = registerWithRole("voucher.staff", "STAFF");
        customer = token(register("voucher.khach"));
        phones = category("Điện thoại voucher", "test-voucher-phones");
        other = category("Danh mục khác voucher", "test-voucher-other");
        phone = variant(product("Điện thoại Voucher", "test-voucher-phone", phones, "14990000"), "Đen", "14990000");
        cover = variant(product("Ốp Voucher", "test-voucher-cover", phones, "200000"), "Trong", "200000");
        StoreFixtures.warehouse(entityManager, phone, cover);
        entityManager.flush();
    }

    @AfterEach
    void revokeTokens() {
        registeredUserIds.forEach(refreshTokenService::revokeAll);
    }

    @Test
    void adminOnly_andTheRulesOfTheForm() throws Exception {
        send(get("/api/v1/admin/vouchers"), staff, null).andExpect(status().isForbidden());
        send(get("/api/v1/admin/vouchers"), customer, null).andExpect(status().isForbidden());
        send(post("/api/v1/admin/vouchers"), staff, body(" poy10test ")).andExpect(status().isForbidden());

        send(post("/api/v1/admin/vouchers"), admin, body(" poy10test "))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.code").value("POY10TEST"))
                .andExpect(jsonPath("$.data.status").value("RUNNING"))
                .andExpect(jsonPath("$.data.usedCount").value(0))
                .andExpect(jsonPath("$.data.perCustomerLimit").value(1));
        send(post("/api/v1/admin/vouchers"), admin, body("POY10TEST"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error.code").value("DUPLICATE_VOUCHER"));
        send(post("/api/v1/admin/vouchers"), admin, body("CÓ DẤU"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.details.code").exists());
        Map<String, Object> backwards = body("NGUOC");
        backwards.put("endsAt", LocalDateTime.now().minusDays(2).truncatedTo(ChronoUnit.SECONDS).toString());
        send(post("/api/v1/admin/vouchers"), admin, backwards)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.details.endsAt").exists());
        Map<String, Object> noCategory = body("KHONGDANHMUC");
        noCategory.put("allCategories", false);
        send(post("/api/v1/admin/vouchers"), admin, noCategory)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.details.categoryIds").exists());
        Map<String, Object> zero = body("KHONGPHANTRAM");
        zero.put("discountPercent", 0);
        send(post("/api/v1/admin/vouchers"), admin, zero).andExpect(status().isBadRequest());
    }

    @Test
    void preview_order_customerLimit_cancelGivesTheUseBack() throws Exception {
        Long id = data(send(post("/api/v1/admin/vouchers"), admin, body("POY10TEST"))).get("id").asLong();
        Map<String, Object> scoped = body("CHIKHAC");
        scoped.put("allCategories", false);
        scoped.put("categoryIds", List.of(other.getId()));
        send(post("/api/v1/admin/vouchers"), admin, scoped).andExpect(status().isCreated());

        // below the 5.000.000đ minimum
        addToCart(cover, 1);
        send(post("/api/v1/vouchers/apply"), customer, Map.of("code", "poy10test"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("VOUCHER_NOT_APPLICABLE"))
                .andExpect(jsonPath("$.error.details.reason").value("BELOW_MINIMUM"))
                .andExpect(jsonPath("$.error.details.shortfall").value("4800000.00"));

        addToCart(phone, 1);
        send(get("/api/v1/vouchers/available"), customer, null)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[?(@.code == 'POY10TEST')].applicable").value(true))
                .andExpect(jsonPath("$.data[?(@.code == 'POY10TEST')].discount").value(500000))
                .andExpect(jsonPath("$.data[?(@.code == 'CHIKHAC')].problem").value("NO_ELIGIBLE_ITEMS"));
        send(post("/api/v1/vouchers/apply"), customer, Map.of("code", " Poy10Test "))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.discount").value(500000))
                .andExpect(jsonPath("$.data.eligibleSubtotal").value(15190000));
        send(post("/api/v1/vouchers/apply"), customer, Map.of("code", "KHONGCO"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error.code").value("VOUCHER_NOT_FOUND"));

        // 15.190.000 − 10% capped at 500.000, free shipping (goods ≥ 10 triệu)
        JsonNode order = data(send(post("/api/v1/orders"), customer, orderBody("poy10test"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.voucherCode").value("POY10TEST"))
                .andExpect(jsonPath("$.data.voucherDiscount").value(500000))
                .andExpect(jsonPath("$.data.subtotal").value(15190000))
                .andExpect(jsonPath("$.data.shippingFee").value(0))
                .andExpect(jsonPath("$.data.total").value(14690000)));
        Long orderId = order.get("id").asLong();
        assertThat(usedCount(id)).isEqualTo(1);
        assertThat(lineShares(orderId)).isEqualByComparingTo("500000");

        // one use per customer
        addToCart(phone, 1);
        send(post("/api/v1/vouchers/apply"), customer, Map.of("code", "POY10TEST"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.details.reason").value("CUSTOMER_LIMIT"));
        send(post("/api/v1/orders"), customer, orderBody("POY10TEST"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("VOUCHER_NOT_APPLICABLE"));

        // the code cannot change and the voucher cannot be deleted once used
        send(put("/api/v1/admin/vouchers/" + id), admin, body("DOIMA"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error.code").value("VOUCHER_IN_USE"));
        send(delete("/api/v1/admin/vouchers/" + id), admin, null)
                .andExpect(status().isConflict());

        // cancelled before delivery → the use counts again
        send(post("/api/v1/orders/" + orderId + "/cancel"), customer, null).andExpect(status().isOk());
        assertThat(usedCount(id)).isZero();
        send(post("/api/v1/vouchers/apply"), customer, Map.of("code", "POY10TEST")).andExpect(status().isOk());
        send(get("/api/v1/admin/vouchers/" + id + "/usages"), admin, null)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content.length()").value(1))
                .andExpect(jsonPath("$.data.content[0].orderId").value(orderId))
                .andExpect(jsonPath("$.data.content[0].releasedAt").exists());

        // switched off → not usable
        send(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch("/api/v1/admin/vouchers/" + id + "/status"),
                admin, Map.of("active", false))
                .andExpect(jsonPath("$.data.status").value("PAUSED"));
        send(post("/api/v1/vouchers/apply"), customer, Map.of("code", "POY10TEST"))
                .andExpect(jsonPath("$.error.details.reason").value("INACTIVE"));
    }

    private int usedCount(Long voucherId) {
        entityManager.flush();
        return ((Number) entityManager.createNativeQuery("select used_count from vouchers where voucher_id = :id")
                .setParameter("id", voucherId).getSingleResult()).intValue();
    }

    private BigDecimal lineShares(Long orderId) {
        entityManager.flush();
        return (BigDecimal) entityManager.createNativeQuery(
                        "select coalesce(sum(voucher_discount), 0) from order_items where order_id = :id")
                .setParameter("id", orderId).getSingleResult();
    }

    private static Map<String, Object> body(String code) {
        Map<String, Object> body = new HashMap<>();
        body.put("code", code);
        body.put("name", "Giảm 10% đơn từ 5 triệu");
        body.put("discountPercent", 10);
        body.put("maxDiscount", 500000);
        body.put("minOrderAmount", 5000000);
        body.put("startsAt", LocalDateTime.now().minusHours(1).truncatedTo(ChronoUnit.SECONDS).toString());
        body.put("endsAt", LocalDateTime.now().plusDays(1).truncatedTo(ChronoUnit.SECONDS).toString());
        body.put("usageLimit", 5);
        return body;
    }

    private static Map<String, Object> orderBody(String voucherCode) {
        Map<String, Object> body = new HashMap<>();
        body.put("recipientName", "Khách Voucher");
        body.put("recipientPhone", "0901234567");
        body.put("shippingAddress", "1 Lê Lợi, Phường Bến Thành, Thành phố Hồ Chí Minh");
        body.put("paymentMethod", "COD");
        body.put("voucherCode", voucherCode);
        return body;
    }

    private void addToCart(ProductVariant variant, int quantity) throws Exception {
        send(post("/api/v1/cart/items"), customer, Map.of("variantId", variant.getId(), "quantity", quantity))
                .andExpect(status().isOk());
    }

    private Category category(String name, String slug) {
        Category category = new Category();
        category.setName(name);
        category.setSlug(slug);
        return categoryRepository.save(category);
    }

    private Product product(String name, String slug, Category category, String price) {
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

    private JsonNode register(String username) throws Exception {
        JsonNode data = data(send(post("/api/v1/auth/register"), null, Map.of(
                "email", username + "@example.com", "username", username, "password", PASSWORD,
                "fullname", "Người Thử Voucher"))
                .andExpect(status().isCreated()));
        registeredUserIds.add(data.get("user").get("id").asLong());
        return data;
    }

    private static String token(JsonNode auth) {
        return auth.get("accessToken").asString();
    }

    private String registerWithRole(String username, String role) throws Exception {
        Long id = register(username).get("user").get("id").asLong();
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
