package com.example.Tech.controller.inventory;

import com.example.Tech.entity.inventory.InventoryId;
import com.example.Tech.entity.inventory.MovementType;
import com.example.Tech.entity.inventory.StockMovement;
import com.example.Tech.entity.product.Category;
import com.example.Tech.entity.product.Product;
import com.example.Tech.entity.product.ProductVariant;
import com.example.Tech.entity.store.Store;
import com.example.Tech.entity.user.UserRole;
import com.example.Tech.repository.inventory.InventoryRepository;
import com.example.Tech.repository.inventory.StockMovementRepository;
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
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * /api/v1/admin/stores/{storeId}/inventory over HTTP against the test database (rolled back): store scoping for
 * STAFF (own store only, read from the DB on every call), stock-in, movement history, filters. Also the ADMIN-only
 * all-stores view /api/v1/admin/inventory.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class StoreInventoryApiIntegrationTest {

    private static final String PASSWORD = "Matkhau@123";

    private static final String OVERVIEW_URL = "/api/v1/admin/inventory";

    private static final String STATS_URL = "/api/v1/admin/inventory/stock-in-stats";

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
    private StockMovementRepository stockMovementRepository;

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
    private String adminToken;
    private String staffToken;
    private String unassignedStaffToken;
    private long staffEmployeeId;
    private Store storeA;
    private Store storeB;
    private ProductVariant phoneBlack;
    private ProductVariant laptopBase;
    private ProductVariant deletedVariant;

    @BeforeEach
    void setUp() throws Exception {
        customerToken = register("tk.customer").get("accessToken").asString();
        adminToken = registerWithRole("tk.admin", "ADMIN");
        staffToken = registerWithRole("tk.staff", "STAFF");
        unassignedStaffToken = registerWithRole("tk.staff2", "STAFF");

        storeA = storeRepository.save(store("ZZ TK Chi nhánh A"));
        storeB = storeRepository.save(store("ZZ TK Chi nhánh B"));

        Category category = new Category();
        category.setName("Tồn kho test");
        category.setSlug("test-tk-category");
        category = categoryRepository.save(category);
        Product phone = productRepository.save(product("Điện thoại TK", "test-tk-phone", category, null));
        Product laptop = productRepository.save(product("Laptop TK", "test-tk-laptop", category, null));
        Product deleted = productRepository.save(product("Đã xoá TK", "test-tk-deleted", category, LocalDateTime.now()));
        phoneBlack = variantRepository.save(variant(phone, "Đen", "TK-PHONE-BLACK"));
        laptopBase = variantRepository.save(variant(laptop, "Bản thường", "TK-LAPTOP"));
        deletedVariant = variantRepository.save(variant(deleted, "Bản cũ", "TK-DELETED"));
        entityManager.flush();

        Long staffUserId = userRepository.findByUsername("tk.staff").orElseThrow().getId();
        staffEmployeeId = data(send(post("/api/v1/admin/employees"), adminToken,
                Map.of("userId", staffUserId, "storeId", storeA.getId(), "positionAtStore", "Quản lý chi nhánh"))
                .andExpect(status().isCreated())).get("id").asLong();
    }

    @AfterEach
    void revokeTokens() {
        registeredUserIds.forEach(refreshTokenService::revokeAll);
    }

    @Test
    void access_staffOnlyTheirOwnStore_adminAnyStore() throws Exception {
        send(get(url(storeA)), null, null).andExpect(status().isUnauthorized());
        send(get(url(storeA)), customerToken, null).andExpect(status().isForbidden());
        send(get(url(storeA)), unassignedStaffToken, null)
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error.code").value("NO_ACTIVE_STORE_ASSIGNMENT"));
        send(get(url(storeB)), staffToken, null)
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error.code").value("ACCESS_DENIED"));
        send(post(url(storeB) + "/stock-in"), staffToken, stockIn(phoneBlack, 1, null))
                .andExpect(status().isForbidden());
        send(get(url(storeA)), staffToken, null).andExpect(status().isOk());
        send(get(url(storeB)), adminToken, null).andExpect(status().isOk());
        send(get("/api/v1/admin/stores/-1/inventory"), adminToken, null)
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error.code").value("STORE_NOT_FOUND"));
    }

    @Test
    void stockIn_addsUp_andTheHistoryShowsWhoAndFromWhom() throws Exception {
        JsonNode first = data(send(post(url(storeA) + "/stock-in"), staffToken, stockIn(phoneBlack, 5, "  Công ty ABC "))
                .andExpect(status().isOk()));
        assertThat(first.get("quantity").asInt()).isEqualTo(5);
        assertThat(first.get("productName").asString()).isEqualTo("Điện thoại TK");
        assertThat(first.get("variantName").asString()).isEqualTo("Đen");

        Map<String, Object> second = stockIn(phoneBlack, 3, null);
        second.put("note", "Bổ sung");
        assertThat(data(send(post(url(storeA) + "/stock-in"), staffToken, second).andExpect(status().isOk()))
                .get("quantity").asInt()).isEqualTo(8);

        assertThat(inventoryRepository.findById(new InventoryId(storeA.getId(), phoneBlack.getId())).orElseThrow()
                .getQuantity()).isEqualTo(8);
        assertThat(inventoryRepository.findByIdStoreIdAndIdVariantId(storeB.getId(), phoneBlack.getId())).isEmpty();

        JsonNode history = data(send(get(url(storeA) + "/" + phoneBlack.getId() + "/movements"), staffToken, null)
                .andExpect(status().isOk())).get("content");
        assertThat(history).hasSize(2);
        assertThat(history.get(0).get("quantityChange").asInt()).isEqualTo(3);
        assertThat(history.get(0).get("note").asString()).isEqualTo("Bổ sung");
        assertThat(history.get(1).get("type").asString()).isEqualTo("IN");
        assertThat(history.get(1).get("supplierName").asString()).isEqualTo("Công ty ABC");
        assertThat(history.get(1).get("createdByName").asString()).isEqualTo("Người Dùng Tồn Kho");
        assertThat(history.get(1).get("orderId").isNull()).isTrue();
    }

    @Test
    void list_filtersByKeywordAndOutOfStock() throws Exception {
        send(post(url(storeA) + "/stock-in"), adminToken, stockIn(phoneBlack, 2, null)).andExpect(status().isOk());
        send(post(url(storeA) + "/stock-in"), adminToken, stockIn(laptopBase, 1, null)).andExpect(status().isOk());
        send(post(url(storeB) + "/stock-in"), adminToken, stockIn(laptopBase, 9, null)).andExpect(status().isOk());
        inventoryRepository.findById(new InventoryId(storeA.getId(), laptopBase.getId())).orElseThrow().setQuantity(0);
        entityManager.flush();

        assertThat(variantIds(storeA)).containsExactly(phoneBlack.getId(), laptopBase.getId());
        assertThat(variantIds(storeA, "keyword", "laptop")).containsExactly(laptopBase.getId());
        assertThat(variantIds(storeA, "keyword", "tk-phone")).containsExactly(phoneBlack.getId());
        assertThat(variantIds(storeA, "outOfStock", "true")).containsExactly(laptopBase.getId());
        assertThat(variantIds(storeB)).containsExactly(laptopBase.getId());
    }

    @Test
    void movingTheStaffToAnotherStore_movesTheirAccessAtOnce() throws Exception {
        send(post("/api/v1/admin/employees/" + staffEmployeeId + "/assignment"), adminToken,
                Map.of("storeId", storeB.getId())).andExpect(status().isOk());

        send(get(url(storeA)), staffToken, null).andExpect(status().isForbidden());
        send(post(url(storeB) + "/stock-in"), staffToken, stockIn(laptopBase, 4, null)).andExpect(status().isOk());
    }

    @Test
    void invalidStockIns() throws Exception {
        send(post(url(storeA) + "/stock-in"), staffToken, stockIn(phoneBlack, 0, null))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.details.quantity").exists());
        send(post(url(storeA) + "/stock-in"), staffToken, Map.of("quantity", 1))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.details.variantId").exists());
        Map<String, Object> unknown = new HashMap<>();
        unknown.put("variantId", -1);
        unknown.put("quantity", 1);
        send(post(url(storeA) + "/stock-in"), staffToken, unknown)
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error.code").value("PRODUCT_VARIANT_NOT_FOUND"));
        send(post(url(storeA) + "/stock-in"), staffToken, stockIn(deletedVariant, 1, null))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error.code").value("PRODUCT_VARIANT_NOT_FOUND"));
        assertThat(inventoryRepository.existsByIdStoreId(storeA.getId())).isFalse();
    }

    @Test
    void overview_isAdminOnly() throws Exception {
        send(get(OVERVIEW_URL), null, null).andExpect(status().isUnauthorized());
        send(get(OVERVIEW_URL), customerToken, null).andExpect(status().isForbidden());
        send(get(OVERVIEW_URL), staffToken, null).andExpect(status().isForbidden());
        send(get(OVERVIEW_URL), adminToken, null).andExpect(status().isOk());
    }

    @Test
    void overview_sumsEveryStore_andOutOfStockMeansNoStoreHasAny() throws Exception {
        send(post(url(storeA) + "/stock-in"), adminToken, stockIn(phoneBlack, 2, null)).andExpect(status().isOk());
        send(post(url(storeA) + "/stock-in"), adminToken, stockIn(laptopBase, 1, null)).andExpect(status().isOk());
        send(post(url(storeB) + "/stock-in"), adminToken, stockIn(laptopBase, 9, null)).andExpect(status().isOk());
        inventoryRepository.findById(new InventoryId(storeA.getId(), laptopBase.getId())).orElseThrow().setQuantity(0);
        entityManager.flush();

        JsonNode rows = overview("keyword", "tk-");
        assertThat(rows).hasSize(2);
        assertThat(rows.get(0).get("variantId").asLong()).isEqualTo(phoneBlack.getId());
        assertThat(rows.get(0).get("totalQuantity").asInt()).isEqualTo(2);
        assertThat(rows.get(0).get("stores")).hasSize(1);
        JsonNode laptop = rows.get(1);
        assertThat(laptop.get("productName").asString()).isEqualTo("Laptop TK");
        assertThat(laptop.get("totalQuantity").asInt()).isEqualTo(9);
        assertThat(laptop.get("stores").get(0).get("storeName").asString()).isEqualTo("ZZ TK Chi nhánh A");
        assertThat(laptop.get("stores").get(0).get("quantity").asInt()).isZero();
        assertThat(laptop.get("stores").get(1).get("quantity").asInt()).isEqualTo(9);
        assertThat(laptop.get("stores").get(1).get("storeActive").asBoolean()).isTrue();

        assertThat(overview("keyword", "tk-", "outOfStock", "true")).isEmpty();
        inventoryRepository.findById(new InventoryId(storeA.getId(), phoneBlack.getId())).orElseThrow().setQuantity(0);
        entityManager.flush();
        JsonNode outOfStock = overview("keyword", "tk-", "outOfStock", "true");
        assertThat(outOfStock).hasSize(1);
        assertThat(outOfStock.get(0).get("variantId").asLong()).isEqualTo(phoneBlack.getId());
    }

    @Test
    void stockInStats_isAdminOnly_andValidatesItsFilter() throws Exception {
        send(get(STATS_URL), null, null).andExpect(status().isUnauthorized());
        send(get(STATS_URL), customerToken, null).andExpect(status().isForbidden());
        send(get(STATS_URL), staffToken, null).andExpect(status().isForbidden());
        send(get(STATS_URL).param("fromDate", "2026-10-08").param("toDate", "2026-10-07"), adminToken, null)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("VALIDATION_ERROR"));
        send(get(STATS_URL).param("storeId", "-1"), adminToken, null)
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error.code").value("STORE_NOT_FOUND"));
    }

    @Test
    void stockInStats_countsOnlyStockIns_perStore_andByDay() throws Exception {
        send(post(url(storeA) + "/stock-in"), adminToken, stockIn(phoneBlack, 5, "  Công ty ABC ")).andExpect(status().isOk());
        send(post(url(storeA) + "/stock-in"), adminToken, stockIn(laptopBase, 3, "công ty abc")).andExpect(status().isOk());
        send(post(url(storeA) + "/stock-in"), adminToken, stockIn(phoneBlack, 2, null)).andExpect(status().isOk());
        send(post(url(storeB) + "/stock-in"), adminToken, stockIn(laptopBase, 9, "Nhà cung cấp B")).andExpect(status().isOk());
        StockMovement out = new StockMovement();
        out.setStore(storeA);
        out.setVariant(phoneBlack);
        out.setMovementType(MovementType.OUT);
        out.setQuantityChange(-4);
        stockMovementRepository.saveAndFlush(out);
        String today = LocalDate.now().toString();

        JsonNode storeAStats = data(send(get(STATS_URL).param("storeId", String.valueOf(storeA.getId()))
                .param("fromDate", today).param("toDate", today), adminToken, null).andExpect(status().isOk()));
        assertThat(storeAStats.get("totalQuantity").asLong()).isEqualTo(10);
        assertThat(storeAStats.get("stockInCount").asLong()).isEqualTo(3);
        assertThat(storeAStats.get("variantCount").asLong()).isEqualTo(2);
        assertThat(storeAStats.get("supplierCount").asLong()).isEqualTo(1);
        assertThat(storeAStats.get("stores")).hasSize(1);
        assertThat(storeAStats.get("stores").get(0).get("quantity").asLong()).isEqualTo(10);
        assertThat(storeAStats.get("stores").get(0).get("lastStockInAt").isNull()).isFalse();

        JsonNode all = data(send(get(STATS_URL).param("fromDate", today), adminToken, null).andExpect(status().isOk()));
        Map<Long, Long> quantities = new HashMap<>();
        all.get("stores").forEach(row -> quantities.put(row.get("storeId").asLong(), row.get("quantity").asLong()));
        assertThat(quantities).containsEntry(storeA.getId().longValue(), 10L).containsEntry(storeB.getId().longValue(), 9L);
        assertThat(all.get("totalQuantity").asLong()).isGreaterThanOrEqualTo(19);

        String tomorrow = LocalDate.now().plusDays(1).toString();
        JsonNode later = data(send(get(STATS_URL).param("storeId", String.valueOf(storeA.getId()))
                .param("fromDate", tomorrow), adminToken, null).andExpect(status().isOk()));
        assertThat(later.get("totalQuantity").asLong()).isZero();
        assertThat(later.get("stores").get(0).get("quantity").asLong()).isZero();
    }

    private JsonNode overview(String... params) throws Exception {
        MockHttpServletRequestBuilder builder = get(OVERVIEW_URL);
        for (int i = 0; i < params.length; i += 2) {
            builder.param(params[i], params[i + 1]);
        }
        return data(send(builder, adminToken, null).andExpect(status().isOk())).get("content");
    }

    private static String url(Store store) {
        return "/api/v1/admin/stores/" + store.getId() + "/inventory";
    }

    private List<Long> variantIds(Store store, String... params) throws Exception {
        MockHttpServletRequestBuilder builder = get(url(store));
        for (int i = 0; i < params.length; i += 2) {
            builder.param(params[i], params[i + 1]);
        }
        List<Long> ids = new ArrayList<>();
        data(send(builder, adminToken, null).andExpect(status().isOk())).get("content")
                .forEach(row -> ids.add(row.get("variantId").asLong()));
        return ids;
    }

    private static Map<String, Object> stockIn(ProductVariant variant, int quantity, String supplierName) {
        Map<String, Object> body = new HashMap<>();
        body.put("variantId", variant.getId());
        body.put("quantity", quantity);
        if (supplierName != null) {
            body.put("supplierName", supplierName);
        }
        return body;
    }

    private static Store store(String name) {
        Store store = new Store();
        store.setName(name);
        store.setAddress("1 Đường Test");
        store.setDistrict("Quận 1");
        store.setCity("Hồ Chí Minh");
        return store;
    }

    private static Product product(String name, String slug, Category category, LocalDateTime deletedAt) {
        Product product = new Product();
        product.setName(name);
        product.setSlug(slug);
        product.setCategory(category);
        product.setBasePrice(new BigDecimal("1000000"));
        product.setDeletedAt(deletedAt);
        return product;
    }

    private static ProductVariant variant(Product product, String name, String sku) {
        ProductVariant variant = new ProductVariant();
        variant.setProduct(product);
        variant.setVariantName(name);
        variant.setSkuVariant(sku);
        variant.setPrice(new BigDecimal("1000000"));
        return variant;
    }

    private JsonNode register(String username) throws Exception {
        JsonNode data = data(send(post("/api/v1/auth/register"), null, Map.of(
                "email", username + "@example.com", "username", username, "password", PASSWORD,
                "fullname", "Người Dùng Tồn Kho"))
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
