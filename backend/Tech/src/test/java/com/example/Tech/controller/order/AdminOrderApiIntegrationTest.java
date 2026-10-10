package com.example.Tech.controller.order;

import com.example.Tech.entity.aftersales.Warranty;
import com.example.Tech.entity.inventory.Inventory;
import com.example.Tech.entity.inventory.InventoryId;
import com.example.Tech.entity.product.Category;
import com.example.Tech.entity.product.Product;
import com.example.Tech.entity.product.ProductVariant;
import com.example.Tech.entity.store.Store;
import com.example.Tech.entity.user.User;
import com.example.Tech.entity.user.UserRole;
import com.example.Tech.entity.sales.SalesRecord;
import com.example.Tech.repository.order.OrderStatusHistoryRepository;
import com.example.Tech.repository.product.CategoryRepository;
import com.example.Tech.repository.product.ProductRepository;
import com.example.Tech.repository.product.ProductVariantRepository;
import com.example.Tech.repository.user.CustomerProfileRepository;
import com.example.Tech.repository.user.RoleRepository;
import com.example.Tech.repository.user.UserRepository;
import com.example.Tech.repository.sales.SalesRecordRepository;
import com.example.Tech.repository.user.UserRoleRepository;
import com.example.Tech.security.JwtTokenService;
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
import java.time.Clock;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.notNullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * /api/v1/admin/orders over HTTP against the test database (rolled back): real STAFF / ADMIN accounts
 * (roles added in the DB, then logged in again so the token carries them), orders placed by a customer
 * through the real checkout. The STAFF account works at store A (Quận 5), where the checkout address sends the
 * orders and where the products are in stock (Phase 7.6). Refresh tokens of the registered users are revoked
 * afterwards.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class AdminOrderApiIntegrationTest {

    private static final String PASSWORD = "Matkhau@123";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JsonMapper jsonMapper;

    @Autowired
    private RefreshTokenService refreshTokenService;

    @Autowired
    private JwtTokenService jwtTokenService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private UserRoleRepository userRoleRepository;

    @Autowired
    private CustomerProfileRepository customerProfileRepository;

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

    @Autowired
    private OrderStatusHistoryRepository historyRepository;

    @Autowired
    private SalesRecordRepository salesRecordRepository;

    private final List<Long> registeredUserIds = new ArrayList<>();

    private Long customerId;
    private String customerToken;
    private String staffToken;
    private Long staffId;
    private String adminToken;
    private ProductVariant phone;
    private ProductVariant cover;
    private ProductVariant spare;
    private Store storeA;
    private Store storeB;

    @BeforeEach
    void setUp() throws Exception {
        JsonNode customer = register("adm.order.customer");
        customerId = customer.get("user").get("id").asLong();
        customerToken = customer.get("accessToken").asString();
        staffToken = registerWithRole("adm.order.staff", "STAFF");
        staffId = registeredUserIds.getLast();
        adminToken = registerWithRole("adm.order.admin", "ADMIN");

        Category category = new Category();
        category.setName("Điện thoại");
        category.setSlug("test-admin-order-dien-thoai");
        category = categoryRepository.save(category);
        Product product = productRepository.save(product("Điện thoại Quản Lý Đơn", "test-admin-order-x", category, "15990000"));
        phone = variantRepository.save(variant(product, "Đen 128GB", "15990000"));
        cover = variantRepository.save(variant(product, "Ốp lưng", "200000"));
        spare = variantRepository.save(variant(product, "Chưa ai mua", "100000"));
        entityManager.flush();

        storeA = StoreFixtures.store(entityManager, "ZZ CN Test Quận 5", "Quận 5");
        storeB = StoreFixtures.store(entityManager, "ZZ CN Test Quận 9", "Quận 9");
        StoreFixtures.assign(entityManager, registeredUserIds.get(1), storeA);
        StoreFixtures.stock(entityManager, storeA, phone, 10);
        StoreFixtures.stock(entityManager, storeA, cover, 10);
    }

    @AfterEach
    void revokeTokens() {
        registeredUserIds.forEach(refreshTokenService::revokeAll);
    }

    @Test
    void access_staffAndAdminOnly_rolesReCheckedInTheDatabase() throws Exception {
        send(get("/api/v1/admin/orders"), null, null)
                .andExpect(status().isUnauthorized());
        send(get("/api/v1/admin/orders"), customerToken, null)
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error.code").value("ACCESS_DENIED"));
        send(get("/api/v1/admin/orders"), staffToken, null).andExpect(status().isOk());
        send(get("/api/v1/admin/orders"), adminToken, null).andExpect(status().isOk());

        // a token that still says STAFF for an account that is only CUSTOMER in the DB
        User customerUser = userRepository.findById(customerId).orElseThrow();
        String staleToken = jwtTokenService.issueAccessToken(customerUser, List.of("STAFF"));
        send(get("/api/v1/admin/orders"), staleToken, null)
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error.code").value("ACCESS_DENIED"));

        // the rest of the admin area stays ADMIN only
        send(get("/api/v1/admin/users"), staffToken, null)
                .andExpect(status().isForbidden());
    }

    @Test
    void search_byStatusCodeTextAndDate_withCustomerAndItems() throws Exception {
        long first = placeOrder(cover, 1, "Nguyễn Văn An", "0901234567");
        long second = placeOrder(phone, 1, "Trần Thị Bình", "0912 345 678");
        patchStatus(second, "CONFIRMED", null).andExpect(status().isOk());
        String today = LocalDate.now(clock).toString();

        send(get("/api/v1/admin/orders"), staffToken, null)
                .andExpect(jsonPath("$.data.totalElements").value(2))
                .andExpect(jsonPath("$.data.content[0].order.id").value(second))
                .andExpect(jsonPath("$.data.content[0].order.items[0].variantId").value(phone.getId()))
                .andExpect(jsonPath("$.data.content[0].customer.id").value(customerId))
                .andExpect(jsonPath("$.data.content[0].customer.username").value("adm.order.customer"))
                .andExpect(jsonPath("$.data.content[0].customer.email").value("adm.order.customer@example.com"));
        assertIds(search("status", "PENDING"), first);
        assertIds(search("status", "CONFIRMED"), second);
        assertIds(search("keyword", "DH%08d".formatted(first)), first);
        assertIds(search("keyword", "bình"), second);
        assertIds(search("keyword", "0912 345"), second);
        assertIds(search("keyword", "adm.order.customer@"), second, first);
        assertIds(search("fromDate", today, "toDate", today), second, first);
        assertIds(search("fromDate", LocalDate.now(clock).plusDays(1).toString()));

        send(get("/api/v1/admin/orders").param("fromDate", today).param("toDate", "2020-01-01"), staffToken, null)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("VALIDATION_ERROR"));
        send(get("/api/v1/admin/orders").param("status", "LOST"), staffToken, null)
                .andExpect(status().isBadRequest());
        send(get("/api/v1/admin/orders/999999999"), staffToken, null)
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error.code").value("ORDER_NOT_FOUND"));
    }

    @Test
    void statusFlow_toDelivered_addsTheTotalToTheCustomer_andTheCustomerSeesIt() throws Exception {
        long orderId = placeOrder(phone, 1, "Nguyễn Văn An", "0901234567");
        BigDecimal spentBefore = customerProfileRepository.findById(customerId).orElseThrow().getTotalSpent();

        patchStatus(orderId, "SHIPPING", null)
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error.code").value("INVALID_ORDER_STATUS"));
        patchStatus(orderId, "CONFIRMED", null)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.order.status").value("CONFIRMED"))
                .andExpect(jsonPath("$.data.order.cancellable").value(false));
        send(post("/api/v1/orders/" + orderId + "/cancel"), customerToken, null)
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error.code").value("INVALID_ORDER_STATUS"));
        patchStatus(orderId, "SHIPPING", "GHN123456")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.order.trackingNumber").value("GHN123456"));
        patchStatus(orderId, "DELIVERED", null)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.order.status").value("DELIVERED"))
                .andExpect(jsonPath("$.data.order.deliveredAt", notNullValue()))
                .andExpect(jsonPath("$.data.order.trackingNumber").value("GHN123456"));

        assertThat(customerProfileRepository.findById(customerId).orElseThrow().getTotalSpent())
                .isEqualByComparingTo(spentBefore.add(new BigDecimal("15990000")));
        // Phase 6: one warranty per line, from the delivery day for the product's 12 months
        List<Warranty> warranties = entityManager.createQuery(
                        "select w from Warranty w where w.orderItem.order.id = :orderId", Warranty.class)
                .setParameter("orderId", orderId).getResultList();
        LocalDate today = LocalDate.now(clock);
        assertThat(warranties).singleElement().satisfies(warranty -> {
            assertThat(warranty.getStartDate()).isEqualTo(today);
            assertThat(warranty.getEndDate()).isEqualTo(today.plusMonths(12));
        });
        // Phase 10: every change is in the history; the sale goes to the confirming staff member, 1% commission
        assertThat(historyRepository.findAllByOrder_IdOrderByChangedAtAscIdAsc(orderId)).extracting(
                        h -> h.getOldStatus() + ">" + h.getNewStatus() + ">" + h.getChangedBy().getId())
                .containsExactly("null>PENDING>" + customerId, "PENDING>CONFIRMED>" + staffId,
                        "CONFIRMED>SHIPPING>" + staffId, "SHIPPING>DELIVERED>" + staffId);
        SalesRecord sale = salesRecordRepository.findByOrder_Id(orderId).orElseThrow();
        assertThat(sale.getEmployee().getUser().getId()).isEqualTo(staffId);
        assertThat(sale.getStore().getId()).isEqualTo(storeA.getId());
        assertThat(sale.getSalesAmount()).isEqualByComparingTo("15990000");
        assertThat(sale.getCommission()).isEqualByComparingTo("159900");
        assertThat(sale.getRecordedAt()).isNotNull();
        send(get("/api/v1/orders/" + orderId), customerToken, null)
                .andExpect(jsonPath("$.data.status").value("DELIVERED"))
                .andExpect(jsonPath("$.data.trackingNumber").value("GHN123456"));
        patchStatus(orderId, "CANCELLED", null)
                .andExpect(status().isConflict());
        send(patch("/api/v1/admin/orders/" + orderId + "/status"), staffToken, Map.of("status", "LOST"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("MALFORMED_REQUEST"));
        send(patch("/api/v1/admin/orders/" + orderId + "/status"), staffToken, Map.of())
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.details.status").exists());
    }

    @Test
    void confirm_takesTheStockOfTheOrdersStore_cancelGivesItBack() throws Exception {
        long orderId = placeOrder(cover, 2, "Nguyễn Văn An", "0901234567");
        assertThat(StoreFixtures.quantity(entityManager, storeA, cover)).isEqualTo(10);

        patchStatus(orderId, "CONFIRMED", null)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.order.storeId").value(storeA.getId()));
        assertThat(StoreFixtures.quantity(entityManager, storeA, cover)).isEqualTo(8);
        assertThat(StoreFixtures.movements(entityManager, orderId)).containsExactly("OUT:-2");

        patchStatus(orderId, "CANCELLED", null)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.order.status").value("CANCELLED"))
                .andExpect(jsonPath("$.data.order.cancelledAt", notNullValue()));
        assertThat(StoreFixtures.quantity(entityManager, storeA, cover)).isEqualTo(10);
        assertThat(StoreFixtures.movements(entityManager, orderId)).containsExactly("OUT:-2", "RETURN:2");

        // a pending order cancelled took nothing and gives nothing back
        long pending = placeOrder(cover, 1, "Nguyễn Văn An", "0901234567");
        patchStatus(pending, "CANCELLED", null).andExpect(status().isOk());
        assertThat(StoreFixtures.movements(entityManager, pending)).isEmpty();
        assertThat(StoreFixtures.quantity(entityManager, storeA, cover)).isEqualTo(10);
    }

    @Test
    void confirm_withoutEnoughStock_is409_adminMovesTheOrderToAStoreThatHasIt() throws Exception {
        entityManager.find(Inventory.class, new InventoryId(storeA.getId(), phone.getId())).setQuantity(1);
        long orderId = placeOrder(phone, 2, "Nguyễn Văn An", "0901234567");

        patchStatus(orderId, "CONFIRMED", null)
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error.code").value("INSUFFICIENT_STOCK"))
                .andExpect(jsonPath("$.error.message").value(
                        "Chi nhánh ZZ CN Test Quận 5 không đủ hàng: Điện thoại Quản Lý Đơn - Đen 128GB (cần 2, còn 1)"));
        send(get("/api/v1/admin/orders/" + orderId), staffToken, null)
                .andExpect(jsonPath("$.data.order.status").value("PENDING"));
        assertThat(StoreFixtures.quantity(entityManager, storeA, phone)).isEqualTo(1);

        // only an ADMIN moves an order, to an existing open store
        send(patch(storeUrl(orderId)), staffToken, Map.of("storeId", storeB.getId()))
                .andExpect(status().isForbidden());
        send(patch(storeUrl(orderId)), adminToken, Map.of())
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.details.storeId").exists());
        send(patch(storeUrl(orderId)), adminToken, Map.of("storeId", 999999))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error.code").value("STORE_NOT_FOUND"));
        setStoreActive(storeB, false);
        send(patch(storeUrl(orderId)), adminToken, Map.of("storeId", storeB.getId()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.details.storeId").exists());
        setStoreActive(storeB, true);
        StoreFixtures.stock(entityManager, storeB, phone, 20);

        send(patch(storeUrl(orderId)), adminToken, Map.of("storeId", storeB.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.order.storeId").value(storeB.getId()))
                .andExpect(jsonPath("$.data.order.storeName").value("ZZ CN Test Quận 9"));
        // the order left the staff member's store
        send(get("/api/v1/admin/orders/" + orderId), staffToken, null)
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error.code").value("ACCESS_DENIED"));
        patchStatus(orderId, "CONFIRMED", null)
                .andExpect(status().isForbidden());
        // the ADMIN only coordinates; store B's own staff confirms
        send(patch("/api/v1/admin/orders/" + orderId + "/status"), adminToken, Map.of("status", "CONFIRMED"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error.code").value("ADMIN_READ_ONLY"));
        String staffB = registerWithRole("adm.order.staffb", "STAFF");
        StoreFixtures.assign(entityManager, registeredUserIds.getLast(), storeB);
        send(patch("/api/v1/admin/orders/" + orderId + "/status"), staffB, Map.of("status", "CONFIRMED"))
                .andExpect(status().isOk());
        assertThat(StoreFixtures.quantity(entityManager, storeB, phone)).isEqualTo(18);
        assertThat(StoreFixtures.quantity(entityManager, storeA, phone)).isEqualTo(1);

        send(patch(storeUrl(orderId)), adminToken, Map.of("storeId", storeA.getId()))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error.code").value("INVALID_ORDER_STATUS"));
    }

    @Test
    void staff_onlySeeTheOrdersOfTheirStore_adminSeesEveryOrderButProcessesNone() throws Exception {
        long own = placeOrder(cover, 1, "Nguyễn Văn An", "0901234567");
        long noStore = placeOrder(cover, 1, "Trần Thị Bình", "0912345678");
        entityManager.createNativeQuery("update orders set store_id = null where order_id = :id")
                .setParameter("id", noStore)
                .executeUpdate();
        entityManager.clear();

        assertIds(search("keyword", "adm.order.customer@"), own);
        send(get("/api/v1/admin/orders").param("storeId", String.valueOf(storeB.getId())), staffToken, null)
                .andExpect(jsonPath("$.data.content[0].order.id").value(own));
        send(get("/api/v1/admin/orders/" + noStore), staffToken, null)
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error.message").value("Đơn DH%08d không thuộc chi nhánh của bạn".formatted(noStore)));
        send(post("/api/v1/admin/orders/" + noStore + "/payment/refund"), staffToken, null)
                .andExpect(status().isForbidden());

        send(get("/api/v1/admin/orders/" + noStore), adminToken, null).andExpect(status().isOk());
        send(get("/api/v1/admin/orders").param("keyword", "adm.order.customer@"), adminToken, null)
                .andExpect(jsonPath("$.data.totalElements").value(2));
        send(patch("/api/v1/admin/orders/" + noStore + "/status"), adminToken, Map.of("status", "CANCELLED"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error.code").value("ADMIN_READ_ONLY"));
        send(post("/api/v1/admin/orders/" + own + "/payment/confirm"), adminToken, Map.of())
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error.code").value("ADMIN_READ_ONLY"));
        send(get("/api/v1/admin/orders/" + noStore), adminToken, null)
                .andExpect(jsonPath("$.data.order.status").value("PENDING"));

        // a STAFF account with no current store reaches no order at all
        String unassigned = registerWithRole("adm.order.staff2", "STAFF");
        send(get("/api/v1/admin/orders"), unassigned, null)
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error.code").value("NO_ACTIVE_STORE_ASSIGNMENT"));
    }

    @Test
    void deletingAVariantThatWasOrdered_isRefusedWith409() throws Exception {
        placeOrder(cover, 1, "Nguyễn Văn An", "0901234567");

        send(delete("/api/v1/product-variants/" + cover.getId()), adminToken, null)
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error.code").value("RESOURCE_IN_USE"));
        send(delete("/api/v1/product-variants/" + spare.getId()), adminToken, null)
                .andExpect(status().isNoContent());
    }

    private long placeOrder(ProductVariant variant, int quantity, String recipient, String phoneNumber) throws Exception {
        send(post("/api/v1/cart/items"), customerToken, Map.of("variantId", variant.getId(), "quantity", quantity))
                .andExpect(status().isOk());
        Map<String, Object> body = new HashMap<>();
        body.put("recipientName", recipient);
        body.put("recipientPhone", phoneNumber);
        body.put("shippingAddress", "12 Nguyễn Trãi, Quận 5, TP. Hồ Chí Minh");
        body.put("paymentMethod", "COD");
        return data(send(post("/api/v1/orders"), customerToken, body).andExpect(status().isCreated())).get("id").asLong();
    }

    private ResultActions patchStatus(long orderId, String newStatus, String trackingNumber) throws Exception {
        Map<String, Object> body = new HashMap<>();
        body.put("status", newStatus);
        if (trackingNumber != null) {
            body.put("trackingNumber", trackingNumber);
        }
        return send(patch("/api/v1/admin/orders/" + orderId + "/status"), staffToken, body);
    }

    /** Native update + clear: the checkout clears the persistence context, so the Store objects here are detached. */
    private void setStoreActive(Store store, boolean active) {
        entityManager.createNativeQuery("update stores set is_active = :active where store_id = :id")
                .setParameter("active", active)
                .setParameter("id", store.getId())
                .executeUpdate();
        entityManager.clear();
    }

    private static String storeUrl(long orderId) {
        return "/api/v1/admin/orders/" + orderId + "/store";
    }

    private JsonNode search(String... params) throws Exception {
        MockHttpServletRequestBuilder builder = get("/api/v1/admin/orders");
        for (int i = 0; i < params.length; i += 2) {
            builder.param(params[i], params[i + 1]);
        }
        return data(send(builder, staffToken, null).andExpect(status().isOk()));
    }

    private static void assertIds(JsonNode page, long... expectedIds) {
        List<Long> ids = new ArrayList<>();
        page.get("content").forEach(row -> ids.add(row.get("order").get("id").asLong()));
        assertThat(ids).containsExactly(java.util.Arrays.stream(expectedIds).boxed().toArray(Long[]::new));
    }

    private JsonNode register(String username) throws Exception {
        JsonNode data = data(send(post("/api/v1/auth/register"), null, Map.of(
                "email", username + "@example.com", "username", username, "password", PASSWORD,
                "fullname", "Người Dùng Đơn Hàng"))
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
            builder.header(HttpHeaders.AUTHORIZATION, token.startsWith("Bearer ") ? token : "Bearer " + token);
        }
        if (body != null) {
            builder.contentType(MediaType.APPLICATION_JSON).content(jsonMapper.writeValueAsString(body));
        }
        return mockMvc.perform(builder);
    }

    private JsonNode data(ResultActions result) throws Exception {
        return jsonMapper.readTree(result.andReturn().getResponse().getContentAsString()).get("data");
    }

    private static Product product(String name, String slug, Category category, String price) {
        Product product = new Product();
        product.setName(name);
        product.setSlug(slug);
        product.setCategory(category);
        product.setBasePrice(new BigDecimal(price));
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
