package com.example.Tech.controller.notification;

import com.example.Tech.entity.audit.AuditLog;
import com.example.Tech.entity.product.Category;
import com.example.Tech.entity.product.Product;
import com.example.Tech.entity.product.ProductVariant;
import com.example.Tech.entity.store.Store;
import com.example.Tech.entity.user.UserRole;
import com.example.Tech.repository.audit.AuditLogRepository;
import com.example.Tech.repository.product.CategoryRepository;
import com.example.Tech.repository.product.ProductRepository;
import com.example.Tech.repository.product.ProductVariantRepository;
import com.example.Tech.repository.store.StoreRepository;
import com.example.Tech.repository.user.RoleRepository;
import com.example.Tech.repository.user.UserRepository;
import com.example.Tech.repository.user.UserRoleRepository;
import com.example.Tech.security.RefreshTokenService;
import com.example.Tech.service.notification.StaffNotificationService;
import com.example.Tech.service.notification.StaffNotificationService.Kind;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Internal notifications over HTTP against the test database (rolled back): who sees what, read state, triggers. */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class StaffNotificationApiIntegrationTest {

    private static final String PASSWORD = "Matkhau@123";
    private static final String URL = "/api/v1/admin/notifications";

    @Autowired private MockMvc mockMvc;
    @Autowired private JsonMapper jsonMapper;
    @Autowired private RefreshTokenService refreshTokenService;
    @Autowired private StaffNotificationService notificationService;
    @Autowired private AuditLogRepository auditLogRepository;
    @Autowired private JdbcTemplate jdbc;
    @Autowired private UserRepository userRepository;
    @Autowired private RoleRepository roleRepository;
    @Autowired private UserRoleRepository userRoleRepository;
    @Autowired private StoreRepository storeRepository;
    @Autowired private CategoryRepository categoryRepository;
    @Autowired private ProductRepository productRepository;
    @Autowired private ProductVariantRepository variantRepository;
    @Autowired private EntityManager entityManager;

    private final List<Long> registeredUserIds = new ArrayList<>();

    private String customerToken;
    private String adminToken;
    private String staffAToken;
    private String staffBToken;
    private String loneStaffToken;
    private Store storeA;
    private Store storeB;
    private ProductVariant variant;

    @BeforeEach
    void setUp() throws Exception {
        jdbc.update("delete from staff_notifications");
        customerToken = register("nt.customer").get("accessToken").asString();
        adminToken = registerWithRole("nt.admin", "ADMIN");
        staffAToken = registerWithRole("nt.staffa", "STAFF");
        staffBToken = registerWithRole("nt.staffb", "STAFF");
        loneStaffToken = registerWithRole("nt.lone", "STAFF");
        storeA = storeRepository.save(store("ZZ NT Chi nhánh A", "Quận Thông Báo A"));
        storeB = storeRepository.save(store("ZZ NT Chi nhánh B", "Quận Thông Báo B"));
        Category category = new Category();
        category.setName("Thông báo test");
        category.setSlug("test-nt-category");
        category = categoryRepository.save(category);
        Product product = new Product();
        product.setName("Điện thoại NT");
        product.setSlug("test-nt-phone");
        product.setCategory(category);
        product.setBasePrice(new BigDecimal("1000000"));
        variant = new ProductVariant();
        variant.setProduct(productRepository.save(product));
        variant.setVariantName("Bản thường");
        variant.setSkuVariant("TK-NT-PHONE");
        variant.setPrice(new BigDecimal("1000000"));
        variant = variantRepository.save(variant);
        entityManager.flush();
        hire("nt.staffa", storeA);
        hire("nt.staffb", storeB);
        auditLogRepository.deleteAll();
        entityManager.flush();
    }

    @AfterEach
    void revokeTokens() {
        registeredUserIds.forEach(refreshTokenService::revokeAll);
    }

    @Test
    void staffSeeTheirOwnStoreAndTheSharedOnes_anAdminSeesAll_aStaffWithoutStoreOnlyTheShared() throws Exception {
        notificationService.publish(storeA, Kind.NEW_ORDER, "Đơn của A", null, null);
        notificationService.publish(storeB, Kind.NEW_ORDER, "Đơn của B", null, null);
        notificationService.publish(null, Kind.SERVICE_REQUEST, "Cho mọi người", null, null);

        assertThat(titles(staffAToken, "")).containsExactlyInAnyOrder("Đơn của A", "Cho mọi người");
        assertThat(titles(staffBToken, "")).containsExactlyInAnyOrder("Đơn của B", "Cho mọi người");
        assertThat(titles(loneStaffToken, "")).containsExactly("Cho mọi người");
        assertThat(titles(adminToken, "")).contains("Đơn của A", "Đơn của B", "Cho mọi người");
        assertThat(unread(staffAToken)).isEqualTo(2);
        assertThat(unread(staffBToken)).isEqualTo(2);
        assertThat(unread(loneStaffToken)).isEqualTo(1);

        JsonNode first = data(send(get(URL), adminToken, null).andExpect(status().isOk())).get("content").get(0);
        assertThat(first.get("title").asString()).isEqualTo("Cho mọi người");
        assertThat(first.get("read").asBoolean()).isFalse();
        assertThat(first.get("storeName").isNull()).isTrue();
    }

    @Test
    void anotherStoresNotificationIsNotFound_andReadStateIsPerPerson() throws Exception {
        notificationService.publish(storeA, Kind.NEW_ORDER, "Đơn của A", "Tổng tiền", "orders.html");
        notificationService.publish(storeB, Kind.NEW_ORDER, "Đơn của B", null, null);
        long idA = idOf(staffAToken, "Đơn của A");
        long idB = idOf(staffBToken, "Đơn của B");

        send(post(URL + "/" + idB + "/read"), staffAToken, null).andExpect(status().isNotFound());
        send(post(URL + "/" + idA + "/read"), staffBToken, null).andExpect(status().isNotFound());
        send(post(URL + "/999999999/read"), staffAToken, null).andExpect(status().isNotFound());
        assertThat(unread(staffAToken)).isEqualTo(1);

        send(post(URL + "/" + idA + "/read"), staffAToken, null).andExpect(status().isOk());
        send(post(URL + "/" + idA + "/read"), staffAToken, null).andExpect(status().isOk());
        assertThat(unread(staffAToken)).isZero();
        assertThat(titles(staffAToken, "?unreadOnly=true")).isEmpty();
        assertThat(titles(staffAToken, "?unreadOnly=false")).containsExactly("Đơn của A");
        // an ADMIN has not read it yet, and reading the other store's one by id is allowed for an ADMIN
        assertThat(titles(adminToken, "?unreadOnly=true")).contains("Đơn của A", "Đơn của B");
        send(post(URL + "/" + idB + "/read"), adminToken, null).andExpect(status().isOk());
        assertThat(titles(adminToken, "?unreadOnly=true")).contains("Đơn của A").doesNotContain("Đơn của B");
        assertThat(unread(staffBToken)).isEqualTo(1);
    }

    @Test
    void readAllOnlyMarksWhatTheCallerCanSee() throws Exception {
        notificationService.publish(storeA, Kind.NEW_ORDER, "Đơn của A", null, null);
        notificationService.publish(storeB, Kind.NEW_ORDER, "Đơn của B", null, null);

        JsonNode marked = data(send(post(URL + "/read-all"), staffAToken, null).andExpect(status().isOk()));
        assertThat(marked.get("marked").asInt()).isEqualTo(1);
        assertThat(unread(staffAToken)).isZero();
        assertThat(unread(staffBToken)).isEqualTo(1);
        assertThat(titles(staffAToken, "")).containsExactly("Đơn của A");
    }

    @Test
    void onlyStaffAccountsMayUseTheBell_andReadingLeavesNoAuditLine() throws Exception {
        send(get(URL), null, null).andExpect(status().isUnauthorized());
        send(get(URL + "/unread-count"), null, null).andExpect(status().isUnauthorized());
        send(get(URL), customerToken, null).andExpect(status().isForbidden());
        send(get(URL + "/unread-count"), customerToken, null).andExpect(status().isForbidden());
        send(post(URL + "/read-all"), customerToken, null).andExpect(status().isForbidden());
        send(post(URL + "/1/read"), customerToken, null).andExpect(status().isForbidden());

        notificationService.publish(storeA, Kind.NEW_ORDER, "Đơn của A", null, null);
        send(post(URL + "/read-all"), staffAToken, null).andExpect(status().isOk());
        List<AuditLog> lines = auditLogRepository.findAll();
        assertThat(lines).isEmpty();
    }

    @Test
    void lowStockIsAnnouncedWhenAVariantCrossesTheLevel_notOnEveryDecrease() {
        notificationService.lowStock(storeA, variant, 20, 10);
        notificationService.lowStock(storeA, variant, 6, 5);
        notificationService.lowStock(storeA, variant, 5, 4);
        notificationService.lowStock(storeA, variant, 4, 0);
        notificationService.lowStock(storeA, variant, 0, 0);
        notificationService.lowStock(storeA, variant, 9, 0);

        List<String> titles = jdbc.queryForList("select title from staff_notifications order by notification_id",
                String.class);
        assertThat(titles).containsExactly(
                "Sắp hết hàng: Điện thoại NT - Bản thường",
                "Hết hàng: Điện thoại NT - Bản thường",
                "Hết hàng: Điện thoại NT - Bản thường");
    }

    @Test
    void sendingAStockTransfer_tellsTheReceivingStore_andTheSenderWhenItRunsLow() throws Exception {
        send(post("/api/v1/admin/stores/" + storeA.getId() + "/inventory/stock-in"), staffAToken,
                Map.of("variantId", variant.getId(), "quantity", 8)).andExpect(status().isOk());
        JsonNode transfer = data(send(post("/api/v1/admin/stock-transfers"), staffAToken, Map.of(
                "fromStoreId", storeA.getId(), "toStoreId", storeB.getId(),
                "items", List.of(Map.of("variantId", variant.getId(), "quantity", 5)))).andExpect(status().isCreated()));
        assertThat(titles(staffBToken, "")).isEmpty();

        send(post("/api/v1/admin/stock-transfers/" + transfer.get("id").asLong() + "/send"), staffAToken, null)
                .andExpect(status().isOk());

        assertThat(titles(staffBToken, "")).hasSize(1).allMatch(title -> title.startsWith("Hàng đang chuyển tới chi nhánh bạn"));
        assertThat(titles(staffAToken, "")).containsExactly("Sắp hết hàng: Điện thoại NT - Bản thường");
        JsonNode lowStock = data(send(get(URL), staffAToken, null).andExpect(status().isOk())).get("content").get(0);
        assertThat(lowStock.get("kind").asString()).isEqualTo("LOW_STOCK");
        assertThat(lowStock.get("link").asString()).isEqualTo("inventory.html");
        assertThat(lowStock.get("storeName").asString()).isEqualTo("ZZ NT Chi nhánh A");
    }

    private List<String> titles(String token, String query) throws Exception {
        JsonNode content = data(send(get(URL + query), token, null).andExpect(status().isOk())).get("content");
        List<String> titles = new ArrayList<>();
        content.forEach(row -> titles.add(row.get("title").asString()));
        return titles;
    }

    private long unread(String token) throws Exception {
        JsonNode count = data(send(get(URL + "/unread-count"), token, null).andExpect(status().isOk()));
        return count.get("unread").asLong();
    }

    private long idOf(String token, String title) throws Exception {
        for (JsonNode row : data(send(get(URL + "?size=50"), token, null).andExpect(status().isOk())).get("content")) {
            if (title.equals(row.get("title").asString())) {
                return row.get("id").asLong();
            }
        }
        throw new AssertionError("Notification not visible: " + title);
    }

    private void hire(String username, Store store) throws Exception {
        Long userId = userRepository.findByUsername(username).orElseThrow().getId();
        send(post("/api/v1/admin/employees"), adminToken,
                Map.of("userId", userId, "storeId", store.getId(), "positionAtStore", "Nhân viên bán hàng"))
                .andExpect(status().isCreated());
    }

    private static Store store(String name, String district) {
        Store store = new Store();
        store.setName(name);
        store.setAddress("1 Đường Test");
        store.setDistrict(district);
        store.setCity("Hồ Chí Minh");
        return store;
    }

    private JsonNode register(String username) throws Exception {
        JsonNode data = data(send(post("/api/v1/auth/register"), null, Map.of(
                "email", username + "@example.com", "username", username, "password", PASSWORD,
                "fullname", "Người Dùng Thông Báo")).andExpect(status().isCreated()));
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
