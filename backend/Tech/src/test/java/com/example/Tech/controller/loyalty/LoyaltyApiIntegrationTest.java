package com.example.Tech.controller.loyalty;

import com.example.Tech.entity.product.Category;
import com.example.Tech.entity.product.Product;
import com.example.Tech.entity.product.ProductVariant;
import com.example.Tech.entity.store.Store;
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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Points earned on delivery, tiers, spending points at checkout, purchase history and "Mua lại". */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class LoyaltyApiIntegrationTest {

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

    private String customer;
    private Long customerId;
    private String staff;
    private String admin;
    private ProductVariant phone;
    private ProductVariant cover;

    @BeforeEach
    void setUp() throws Exception {
        JsonNode auth = register("loyal.khach");
        customer = auth.get("accessToken").asString();
        customerId = auth.get("user").get("id").asLong();
        staff = registerWithRole("loyal.staff", "STAFF");
        Long staffId = registeredUserIds.getLast();
        admin = registerWithRole("loyal.admin", "ADMIN");
        Category category = new Category();
        category.setName("Danh mục điểm");
        category.setSlug("test-loyalty-category");
        categoryRepository.save(category);
        phone = variant(product("Điện thoại Điểm", "test-loyalty-phone", category), "Đen", "15990000");
        cover = variant(product("Ốp Lưng Điểm", "test-loyalty-cover", category), "Trong", "200000");
        entityManager.flush();
        Store store = StoreFixtures.store(entityManager, "ZZ CN Điểm Quận 5", "Quận 5");
        StoreFixtures.assign(entityManager, staffId, store);
        StoreFixtures.stock(entityManager, store, phone, 20);
        StoreFixtures.stock(entityManager, store, cover, 20);
    }

    @AfterEach
    void revokeTokens() {
        registeredUserIds.forEach(refreshTokenService::revokeAll);
    }

    @Test
    void newCustomer_isMember_withTheTiersAndRules() throws Exception {
        send(get("/api/v1/loyalty/me"), customer, null)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.points").value(0))
                .andExpect(jsonPath("$.data.tier.code").value("MEMBER"))
                .andExpect(jsonPath("$.data.nextTier.code").value("SILVER"))
                .andExpect(jsonPath("$.data.nextTier.minSpent").value(50000000))
                .andExpect(jsonPath("$.data.amountToNextTier").value(50000000))
                .andExpect(jsonPath("$.data.progressPercent").value(0))
                .andExpect(jsonPath("$.data.tiers.length()").value(4))
                .andExpect(jsonPath("$.data.tiers[3].code").value("DIAMOND"))
                .andExpect(jsonPath("$.data.tiers[3].minSpent").value(300000000))
                .andExpect(jsonPath("$.data.rules.vndPerPointEarned").value(10000))
                .andExpect(jsonPath("$.data.rules.maxRedeemPercent").value(10));
        send(get("/api/v1/loyalty/me"), null, null).andExpect(status().isUnauthorized());
        send(get("/api/v1/loyalty/me/history"), customer, null).andExpect(jsonPath("$.data.content.length()").value(0));
    }

    @Test
    void delivery_earnsPoints_withTheTierMultiplier_andTheTierGoesUp() throws Exception {
        long first = placeOrder(Map.of(phone, 1), null);
        deliver(first);

        send(get("/api/v1/orders/" + first), customer, null).andExpect(jsonPath("$.data.pointsEarned").value(1599));
        send(get("/api/v1/loyalty/me"), customer, null)
                .andExpect(jsonPath("$.data.points").value(1599))
                .andExpect(jsonPath("$.data.totalSpent").value(15990000))
                .andExpect(jsonPath("$.data.progressPercent").value(31));
        send(get("/api/v1/loyalty/me/history"), customer, null)
                .andExpect(jsonPath("$.data.content[0].type").value("EARN"))
                .andExpect(jsonPath("$.data.content[0].points").value(1599))
                .andExpect(jsonPath("$.data.content[0].balanceAfter").value(1599))
                .andExpect(jsonPath("$.data.content[0].orderId").value(first));

        // ADMIN lowers Bạc to 10tr → the customer moves up now; the next delivery earns x1.2
        send(put("/api/v1/admin/loyalty/tiers"), admin, tiers("0", "10000000", "150000000", "300000000"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[1].minSpent").value(10000000));
        send(get("/api/v1/loyalty/me"), customer, null).andExpect(jsonPath("$.data.tier.code").value("SILVER"));
        long second = placeOrder(Map.of(phone, 1), null);
        deliver(second);
        send(get("/api/v1/orders/" + second), customer, null).andExpect(jsonPath("$.data.pointsEarned").value(1918));
        send(get("/api/v1/loyalty/me"), customer, null).andExpect(jsonPath("$.data.points").value(1599 + 1918));

        // raising the threshold again never takes the tier away
        send(put("/api/v1/admin/loyalty/tiers"), admin, tiers("0", "50000000", "150000000", "300000000")).andExpect(status().isOk());
        send(get("/api/v1/loyalty/me"), customer, null).andExpect(jsonPath("$.data.tier.code").value("SILVER"));
    }

    @Test
    void tierSettings_adminOnly_andChecked() throws Exception {
        send(get("/api/v1/admin/loyalty/tiers"), staff, null).andExpect(status().isForbidden());
        send(put("/api/v1/admin/loyalty/tiers"), customer, tiers("0", "1", "2", "3")).andExpect(status().isForbidden());
        send(get("/api/v1/admin/loyalty/tiers"), admin, null).andExpect(jsonPath("$.data.length()").value(4));
        send(put("/api/v1/admin/loyalty/tiers"), admin, tiers("5", "50000000", "150000000", "300000000"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.details['tiers.MEMBER.minSpent']").exists());
        send(put("/api/v1/admin/loyalty/tiers"), admin, tiers("0", "50000000", "50000000", "300000000"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.details['tiers.GOLD.minSpent']").exists());
        Map<String, Object> partial = tiers("0", "50000000", "150000000", "300000000");
        ((List<?>) partial.get("tiers")).remove(3);
        send(put("/api/v1/admin/loyalty/tiers"), admin, partial).andExpect(status().isBadRequest());
        Map<String, Object> wild = tiers("0", "50000000", "150000000", "300000000");
        ((Map<String, Object>) ((List<?>) wild.get("tiers")).get(1)).put("pointMultiplier", 6);
        send(put("/api/v1/admin/loyalty/tiers"), admin, wild).andExpect(status().isBadRequest());
    }

    @Test
    void spendingPoints_atMostTenPercentAndTheBalance_givenBackOnCancel() throws Exception {
        entityManager.createNativeQuery("update customer_profiles set loyalty_points = 500 where customer_id = :id")
                .setParameter("id", customerId).executeUpdate();
        addToCart(cover, 1);
        send(post("/api/v1/orders"), customer, orderBody(21))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.details.pointsToUse").value("Đơn này dùng được tối đa 20 điểm (10% giá trị hàng)"));
        long orderId = data(send(post("/api/v1/orders"), customer, orderBody(20)).andExpect(status().isCreated())).get("id").asLong();
        JsonNode order = data(send(get("/api/v1/orders/" + orderId), customer, null));
        assertThat(order.get("pointsUsed").asInt()).isEqualTo(20);
        assertThat(order.get("pointsDiscount").decimalValue()).isEqualByComparingTo("20000");
        assertThat(order.get("total").decimalValue()).isEqualByComparingTo(new BigDecimal("200000").subtract(new BigDecimal("20000"))
                .add(order.get("shippingFee").decimalValue()));
        send(get("/api/v1/loyalty/me"), customer, null).andExpect(jsonPath("$.data.points").value(480));
        send(get("/api/v1/loyalty/me/history"), customer, null)
                .andExpect(jsonPath("$.data.content[0].type").value("REDEEM"))
                .andExpect(jsonPath("$.data.content[0].points").value(-20));

        addToCart(phone, 1);
        send(post("/api/v1/orders"), customer, orderBody(600))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.details.pointsToUse").value("Bạn chỉ có 480 điểm"));

        send(post("/api/v1/orders/" + orderId + "/cancel"), customer, null).andExpect(status().isOk());
        send(get("/api/v1/loyalty/me"), customer, null).andExpect(jsonPath("$.data.points").value(500));
        send(get("/api/v1/loyalty/me/history"), customer, null)
                .andExpect(jsonPath("$.data.content[0].type").value("REDEEM_BACK"))
                .andExpect(jsonPath("$.data.content[0].points").value(20))
                .andExpect(jsonPath("$.data.content[0].balanceAfter").value(500));
    }

    @Test
    void refundedReturn_givesBackSpentPoints_andTakesBackEarnedOnes() throws Exception {
        entityManager.createNativeQuery("update customer_profiles set loyalty_points = 1000 where customer_id = :id")
                .setParameter("id", customerId).executeUpdate();
        long orderId = placeOrder(Map.of(phone, 1), 1000);
        deliver(orderId);
        JsonNode order = data(send(get("/api/v1/orders/" + orderId), customer, null));
        assertThat(order.get("total").decimalValue()).isEqualByComparingTo("14990000");
        assertThat(order.get("pointsEarned").asInt()).isEqualTo(1499);
        send(get("/api/v1/loyalty/me"), customer, null).andExpect(jsonPath("$.data.points").value(1499));

        long lineId = order.get("items").get(0).get("orderItemId").asLong();
        long returnId = data(send(post("/api/v1/return-requests"), customer, Map.of("orderId", orderId, "reasonType", "DEFECTIVE",
                "reason", "Máy lỗi màn hình khi mở hộp.", "items", List.of(Map.of("orderItemId", lineId, "quantity", 1))))
                .andExpect(status().isCreated())).get("id").asLong();
        String url = "/api/v1/admin/service-requests/RETURN/" + returnId;
        send(patch(url), staff, Map.of("status", "APPROVED")).andExpect(status().isOk());
        send(patch(url), staff, Map.of("status", "RECEIVED", "restockOrderItemIds", List.of(lineId))).andExpect(status().isOk());
        send(patch(url), staff, Map.of("status", "REFUNDED")).andExpect(status().isOk())
                .andExpect(jsonPath("$.data.refundAmount").value(14990000));

        send(get("/api/v1/loyalty/me"), customer, null).andExpect(jsonPath("$.data.points").value(1000));
        send(get("/api/v1/loyalty/me/history"), customer, null)
                .andExpect(jsonPath("$.data.content[0].type").value("REVOKE"))
                .andExpect(jsonPath("$.data.content[0].points").value(-1499))
                .andExpect(jsonPath("$.data.content[1].type").value("REDEEM_BACK"))
                .andExpect(jsonPath("$.data.content[1].points").value(1000));
    }

    @Test
    void history_byStatusAndKeyword_counts_andBuyAgain() throws Exception {
        long delivered = placeOrder(Map.of(phone, 1, cover, 2), null);
        deliver(delivered);
        long cancelled = placeOrder(Map.of(cover, 3), null);
        send(post("/api/v1/orders/" + cancelled + "/cancel"), customer, null).andExpect(status().isOk());
        long pending = placeOrder(Map.of(cover, 1), null);

        send(get("/api/v1/orders/summary"), customer, null)
                .andExpect(jsonPath("$.data.all").value(3))
                .andExpect(jsonPath("$.data.pending").value(1))
                .andExpect(jsonPath("$.data.inProgress").value(0))
                .andExpect(jsonPath("$.data.delivered").value(1))
                .andExpect(jsonPath("$.data.cancelled").value(1));
        send(get("/api/v1/orders?status=CANCELLED"), customer, null)
                .andExpect(jsonPath("$.data.totalElements").value(1))
                .andExpect(jsonPath("$.data.content[0].id").value(cancelled));
        send(get("/api/v1/orders?status=CONFIRMED&status=SHIPPING"), customer, null)
                .andExpect(jsonPath("$.data.totalElements").value(0));
        send(get("/api/v1/orders?keyword=điện thoại điểm"), customer, null)
                .andExpect(jsonPath("$.data.totalElements").value(1))
                .andExpect(jsonPath("$.data.content[0].id").value(delivered));
        send(get("/api/v1/orders?keyword=DH" + String.format("%08d", pending)), customer, null)
                .andExpect(jsonPath("$.data.totalElements").value(1))
                .andExpect(jsonPath("$.data.content[0].id").value(pending));
        send(get("/api/v1/orders?status=NOPE"), customer, null).andExpect(status().isBadRequest());

        // buy again: the cover's product is no longer sold → skipped with its reason, the phone goes into the cart
        entityManager.createNativeQuery("update products set is_active = false where product_id = :id")
                .setParameter("id", cover.getProduct().getId()).executeUpdate();
        entityManager.clear();
        send(post("/api/v1/orders/" + pending + "/reorder"), customer, null)
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error.code").value("INVALID_ORDER_STATUS"));
        send(post("/api/v1/orders/" + delivered + "/reorder"), staff, null).andExpect(status().isNotFound());
        send(post("/api/v1/orders/" + delivered + "/reorder"), customer, null)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.added.length()").value(1))
                .andExpect(jsonPath("$.data.added[0].variantId").value(phone.getId()))
                .andExpect(jsonPath("$.data.skipped.length()").value(1))
                .andExpect(jsonPath("$.data.skipped[0].variantId").value(cover.getId()))
                .andExpect(jsonPath("$.data.skipped[0].reason").value("Sản phẩm đã ngừng bán"))
                .andExpect(jsonPath("$.data.cartQuantity").value(1));
        send(post("/api/v1/orders/" + cancelled + "/reorder"), customer, null)
                .andExpect(jsonPath("$.data.added.length()").value(0))
                .andExpect(jsonPath("$.data.skipped.length()").value(1));
    }

    private long placeOrder(Map<ProductVariant, Integer> lines, Integer points) throws Exception {
        for (Map.Entry<ProductVariant, Integer> line : lines.entrySet()) {
            addToCart(line.getKey(), line.getValue());
        }
        return data(send(post("/api/v1/orders"), customer, orderBody(points)).andExpect(status().isCreated())).get("id").asLong();
    }

    private void deliver(long orderId) throws Exception {
        for (String next : List.of("CONFIRMED", "SHIPPING", "DELIVERED")) {
            send(patch("/api/v1/admin/orders/" + orderId + "/status"), staff, Map.of("status", next)).andExpect(status().isOk());
        }
    }

    private void addToCart(ProductVariant variant, int quantity) throws Exception {
        send(post("/api/v1/cart/items"), customer, Map.of("variantId", variant.getId(), "quantity", quantity))
                .andExpect(status().isOk());
    }

    private static Map<String, Object> orderBody(Integer points) {
        Map<String, Object> body = new HashMap<>();
        body.put("recipientName", "Khách Điểm");
        body.put("recipientPhone", "0901234567");
        body.put("shippingAddress", "12 Nguyễn Trãi, Quận 5, TP. Hồ Chí Minh");
        body.put("paymentMethod", "COD");
        if (points != null) {
            body.put("pointsToUse", points);
        }
        return body;
    }

    private static Map<String, Object> tiers(String member, String silver, String gold, String diamond) {
        List<Map<String, Object>> list = new ArrayList<>();
        list.add(new HashMap<>(Map.of("code", "MEMBER", "minSpent", member, "pointMultiplier", "1")));
        list.add(new HashMap<>(Map.of("code", "SILVER", "minSpent", silver, "pointMultiplier", "1.2")));
        list.add(new HashMap<>(Map.of("code", "GOLD", "minSpent", gold, "pointMultiplier", "1.5")));
        list.add(new HashMap<>(Map.of("code", "DIAMOND", "minSpent", diamond, "pointMultiplier", "2")));
        Map<String, Object> body = new HashMap<>();
        body.put("tiers", list);
        return body;
    }

    private Product product(String name, String slug, Category category) {
        Product product = new Product();
        product.setName(name);
        product.setSlug(slug);
        product.setCategory(category);
        product.setBasePrice(new BigDecimal("1000"));
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
                "fullname", "Người Thử Điểm"))
                .andExpect(status().isCreated()));
        registeredUserIds.add(data.get("user").get("id").asLong());
        return data;
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
