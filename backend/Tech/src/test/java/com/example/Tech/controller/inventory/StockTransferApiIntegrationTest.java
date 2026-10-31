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
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Stock transfers between two stores over HTTP against the test database (rolled back). */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class StockTransferApiIntegrationTest {

    private static final String PASSWORD = "Matkhau@123";
    private static final String URL = "/api/v1/admin/stock-transfers";

    @Autowired private MockMvc mockMvc;
    @Autowired private JsonMapper jsonMapper;
    @Autowired private RefreshTokenService refreshTokenService;
    @Autowired private UserRepository userRepository;
    @Autowired private RoleRepository roleRepository;
    @Autowired private UserRoleRepository userRoleRepository;
    @Autowired private StoreRepository storeRepository;
    @Autowired private InventoryRepository inventoryRepository;
    @Autowired private StockMovementRepository stockMovementRepository;
    @Autowired private CategoryRepository categoryRepository;
    @Autowired private ProductRepository productRepository;
    @Autowired private ProductVariantRepository variantRepository;
    @Autowired private EntityManager entityManager;

    private final List<Long> registeredUserIds = new ArrayList<>();

    private String customerToken;
    private String adminToken;
    private String staffA;
    private String staffB;
    private String staffC;
    private Store storeA;
    private Store storeB;
    private Store storeC;
    private ProductVariant phone;
    private ProductVariant laptop;

    @BeforeEach
    void setUp() throws Exception {
        customerToken = register("ck.customer").get("accessToken").asString();
        adminToken = registerWithRole("ck.admin", "ADMIN");
        staffA = registerWithRole("ck.staff.a", "STAFF");
        staffB = registerWithRole("ck.staff.b", "STAFF");
        staffC = registerWithRole("ck.staff.c", "STAFF");
        storeA = storeRepository.save(store("ZZ CK Chi nhánh A"));
        storeB = storeRepository.save(store("ZZ CK Chi nhánh B"));
        storeC = storeRepository.save(store("ZZ CK Chi nhánh C"));

        Category category = new Category();
        category.setName("Chuyển kho test");
        category.setSlug("test-ck-category");
        category = categoryRepository.save(category);
        phone = variantRepository.save(variant(productRepository.save(product("Điện thoại CK", "test-ck-phone", category)), "TK-CK-PHONE"));
        laptop = variantRepository.save(variant(productRepository.save(product("Laptop CK", "test-ck-laptop", category)), "TK-CK-LAPTOP"));
        entityManager.flush();

        assign("ck.staff.a", storeA);
        assign("ck.staff.b", storeB);
        assign("ck.staff.c", storeC);
        stock(storeA, phone, 10);
        stock(storeA, laptop, 2);
    }

    @AfterEach
    void revokeTokens() {
        registeredUserIds.forEach(refreshTokenService::revokeAll);
    }

    @Test
    void sendThenReceive_movesTheStock_andLeavesTheLedgerOnBothSides() throws Exception {
        long id = createDraft(staffA, storeA, storeB, line(phone, 4));
        assertThat(quantity(storeA, phone)).isEqualTo(10);

        JsonNode sent = data(send(post(URL + "/" + id + "/send"), staffA, null).andExpect(status().isOk()));
        assertThat(sent.get("status").asString()).isEqualTo("IN_TRANSIT");
        assertThat(sent.get("sentByName").asString()).isNotBlank();
        assertThat(sent.get("code").asString()).isEqualTo("CK%06d".formatted(id));
        assertThat(quantity(storeA, phone)).isEqualTo(6);
        assertThat(inventoryRepository.findById(new InventoryId(storeB.getId(), phone.getId()))).isEmpty();

        // the receiving side cannot send, cancel or edit; the sending side cannot receive
        send(post(URL + "/" + id + "/receive"), staffA, null).andExpect(status().isForbidden());
        send(post(URL + "/" + id + "/cancel"), staffB, null).andExpect(status().isForbidden());
        send(post(URL + "/" + id + "/receive"), staffC, null).andExpect(status().isForbidden());

        JsonNode received = data(send(post(URL + "/" + id + "/receive"), staffB, null).andExpect(status().isOk()));
        assertThat(received.get("status").asString()).isEqualTo("RECEIVED");
        assertThat(received.get("receivedByName").asString()).isNotBlank();
        assertThat(quantity(storeA, phone)).isEqualTo(6);
        assertThat(quantity(storeB, phone)).isEqualTo(4);

        List<StockMovement> out = movements(storeA, phone);
        List<StockMovement> in = movements(storeB, phone);
        assertThat(out).extracting(StockMovement::getMovementType, StockMovement::getQuantityChange, StockMovement::getTransferId)
                .contains(org.assertj.core.groups.Tuple.tuple(MovementType.TRANSFER_OUT, -4, id));
        assertThat(in).extracting(StockMovement::getMovementType, StockMovement::getQuantityChange, StockMovement::getTransferId)
                .containsExactly(org.assertj.core.groups.Tuple.tuple(MovementType.TRANSFER_IN, 4, id));

        send(post(URL + "/" + id + "/receive"), staffB, null)
                .andExpect(status().isConflict()).andExpect(jsonPath("$.error.code").value("STOCK_TRANSFER_INVALID_STATUS"));
        send(post(URL + "/" + id + "/cancel"), staffA, null).andExpect(status().isConflict());
        send(post(URL + "/" + id + "/send"), staffA, null).andExpect(status().isConflict());
    }

    @Test
    void send_withTooLittleStock_changesNothing_andNamesEveryShortLine() throws Exception {
        long id = createDraft(staffA, storeA, storeB, line(phone, 3), line(laptop, 5));
        JsonNode error = jsonMapper.readTree(send(post(URL + "/" + id + "/send"), staffA, null)
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error.code").value("STOCK_TRANSFER_INSUFFICIENT_STOCK"))
                .andReturn().getResponse().getContentAsString()).get("error");
        assertThat(error.get("message").asString()).contains("Laptop CK").contains("cần 5, còn 2").doesNotContain("Điện thoại CK");
        assertThat(quantity(storeA, phone)).isEqualTo(10);
        assertThat(quantity(storeA, laptop)).isEqualTo(2);
        assertThat(data(send(get(URL + "/" + id), staffA, null).andExpect(status().isOk())).get("status").asString()).isEqualTo("DRAFT");
    }

    @Test
    void cancel_aDraftChangesNothing_aTransferOnTheRoadGivesTheGoodsBack() throws Exception {
        long draft = createDraft(staffA, storeA, storeB, line(phone, 2));
        send(post(URL + "/" + draft + "/cancel"), staffA, null)
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.status").value("CANCELLED"));
        assertThat(quantity(storeA, phone)).isEqualTo(10);

        long onTheRoad = createDraft(staffA, storeA, storeB, line(phone, 7));
        send(post(URL + "/" + onTheRoad + "/send"), staffA, null).andExpect(status().isOk());
        assertThat(quantity(storeA, phone)).isEqualTo(3);
        send(post(URL + "/" + onTheRoad + "/cancel"), staffA, null)
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.status").value("CANCELLED"))
                .andExpect(jsonPath("$.data.cancelledByName").isNotEmpty());
        assertThat(quantity(storeA, phone)).isEqualTo(10);
        assertThat(inventoryRepository.findById(new InventoryId(storeB.getId(), phone.getId()))).isEmpty();
        send(post(URL + "/" + onTheRoad + "/receive"), staffB, null).andExpect(status().isConflict());
        assertThat(movements(storeA, phone)).extracting(StockMovement::getMovementType)
                .contains(MovementType.TRANSFER_OUT, MovementType.TRANSFER_IN);
    }

    @Test
    void draft_canBeReplaced_onlyBeforeItIsSent() throws Exception {
        long id = createDraft(staffA, storeA, storeB, line(phone, 2));
        JsonNode replaced = data(send(put(URL + "/" + id), staffA, body(storeA, storeC, line(laptop, 1), line(phone, 5)))
                .andExpect(status().isOk()));
        assertThat(replaced.get("toStoreId").asInt()).isEqualTo(storeC.getId());
        assertThat(replaced.get("items")).hasSize(2);
        assertThat(replaced.get("totalQuantity").asLong()).isEqualTo(6);
        send(put(URL + "/" + id), staffB, body(storeA, storeB, line(phone, 1))).andExpect(status().isForbidden());

        send(post(URL + "/" + id + "/send"), staffA, null).andExpect(status().isOk());
        send(put(URL + "/" + id), staffA, body(storeA, storeB, line(phone, 1)))
                .andExpect(status().isConflict()).andExpect(jsonPath("$.error.code").value("STOCK_TRANSFER_INVALID_STATUS"));
    }

    @Test
    void validation_andStoreScoping() throws Exception {
        send(get(URL), null, null).andExpect(status().isUnauthorized());
        send(get(URL), customerToken, null).andExpect(status().isForbidden());
        // staff only create from their own store
        send(post(URL), staffB, body(storeA, storeB, line(phone, 1))).andExpect(status().isForbidden());
        send(post(URL), staffA, body(storeA, storeA, line(phone, 1))).andExpect(status().isBadRequest());
        send(post(URL), staffA, body(storeA, storeB)).andExpect(status().isBadRequest());
        send(post(URL), staffA, body(storeA, storeB, line(phone, 0))).andExpect(status().isBadRequest());
        send(post(URL), staffA, body(storeA, storeB, line(phone, 1), line(phone, 2))).andExpect(status().isBadRequest());
        send(post(URL), staffA, body(storeA, storeB, Map.of("variantId", 99999999L, "quantity", 1)))
                .andExpect(status().isNotFound()).andExpect(jsonPath("$.error.code").value("PRODUCT_VARIANT_NOT_FOUND"));
        storeB.setActive(false);
        entityManager.flush();
        send(post(URL), staffA, body(storeA, storeB, line(phone, 1))).andExpect(status().isBadRequest());
        storeB.setActive(true);
        entityManager.flush();

        // a third store cannot read the transfer; the two sides and the ADMIN can
        long id = createDraft(staffA, storeA, storeB, line(phone, 1));
        send(get(URL + "/" + id), staffC, null).andExpect(status().isForbidden());
        send(get(URL + "/" + id), staffA, null).andExpect(status().isOk());
        // a draft stays private to the sending store until it is sent
        send(get(URL + "/" + id), staffB, null).andExpect(status().isForbidden());
        send(post(URL + "/" + id + "/send"), staffA, null).andExpect(status().isOk());
        send(get(URL + "/" + id), staffB, null).andExpect(status().isOk());
        send(get(URL + "/" + id), adminToken, null).andExpect(status().isOk());
        send(get(URL + "/-1"), adminToken, null)
                .andExpect(status().isNotFound()).andExpect(jsonPath("$.error.code").value("STOCK_TRANSFER_NOT_FOUND"));
    }

    @Test
    void adminDoesEveryStep_andListsAreScopedByStoreAndDirection() throws Exception {
        long one = createDraft(staffA, storeA, storeB, line(phone, 1));
        long two = createDraft(staffB, storeB, storeA, line(phone, 1));
        long three = createDraft(adminToken, storeA, storeC, line(phone, 1));
        send(post(URL + "/" + three + "/send"), adminToken, null).andExpect(status().isOk());
        send(post(URL + "/" + three + "/receive"), adminToken, null).andExpect(status().isOk());
        assertThat(quantity(storeC, phone)).isEqualTo(1);

        // drafts are listed for the sending store only
        assertThat(ids(staffA, "")).containsExactlyInAnyOrder(one, three);
        assertThat(ids(staffA, "?direction=OUTGOING")).containsExactlyInAnyOrder(one, three);
        assertThat(ids(staffA, "?direction=INCOMING")).isEmpty();
        assertThat(ids(staffB, "")).containsExactly(two);
        send(post(URL + "/" + one + "/send"), staffA, null).andExpect(status().isOk());
        assertThat(ids(staffB, "?direction=INCOMING")).containsExactly(one);
        assertThat(ids(staffC, "")).containsExactly(three);
        assertThat(ids(staffA, "?status=RECEIVED")).containsExactly(three);
        assertThat(ids(adminToken, "")).contains(one, two, three);
        assertThat(ids(adminToken, "?storeId=" + storeC.getId())).containsExactly(three);
        assertThat(data(send(get(URL), staffA, null)).get("content").get(0).get("items")).isEmpty();
    }

    private List<Long> ids(String token, String query) throws Exception {
        List<Long> ids = new ArrayList<>();
        data(send(get(URL + query), token, null).andExpect(status().isOk())).get("content")
                .forEach(node -> ids.add(node.get("id").asLong()));
        return ids;
    }

    @SafeVarargs
    private long createDraft(String token, Store from, Store to, Map<String, Object>... lines) throws Exception {
        return data(send(post(URL), token, body(from, to, lines)).andExpect(status().isCreated())).get("id").asLong();
    }

    private Map<String, Object> line(ProductVariant variant, int quantity) {
        return Map.of("variantId", variant.getId(), "quantity", quantity);
    }

    @SafeVarargs
    private static Map<String, Object> body(Store from, Store to, Map<String, Object>... lines) {
        return Map.of("fromStoreId", from.getId(), "toStoreId", to.getId(), "note", "Điều hàng thử", "items", List.of(lines));
    }

    private int quantity(Store store, ProductVariant variant) {
        entityManager.flush();
        entityManager.clear();
        return inventoryRepository.findById(new InventoryId(store.getId(), variant.getId())).map(row -> row.getQuantity()).orElse(0);
    }

    private List<StockMovement> movements(Store store, ProductVariant variant) {
        entityManager.flush();
        return stockMovementRepository.findByStoreIdAndVariantIdOrderByCreatedAtDescIdDesc(
                store.getId(), variant.getId(), org.springframework.data.domain.Pageable.unpaged()).getContent();
    }

    private void stock(Store store, ProductVariant variant, int quantity) {
        inventoryRepository.insertIfMissing(store.getId(), variant.getId());
        inventoryRepository.findAllForUpdate(store.getId(), List.of(variant.getId())).getFirst().setQuantity(quantity);
        entityManager.flush();
    }

    private void assign(String username, Store store) throws Exception {
        Long userId = userRepository.findByUsername(username).orElseThrow().getId();
        send(post("/api/v1/admin/employees"), adminToken,
                Map.of("userId", userId, "storeId", store.getId(), "positionAtStore", "Nhân viên bán hàng"))
                .andExpect(status().isCreated());
    }

    private static Store store(String name) {
        Store store = new Store();
        store.setName(name);
        store.setAddress("1 Đường Test");
        store.setDistrict("Quận 1");
        store.setCity("Hồ Chí Minh");
        return store;
    }

    private static Product product(String name, String slug, Category category) {
        Product product = new Product();
        product.setName(name);
        product.setSlug(slug);
        product.setCategory(category);
        product.setBasePrice(new BigDecimal("1000000"));
        return product;
    }

    private static ProductVariant variant(Product product, String sku) {
        ProductVariant variant = new ProductVariant();
        variant.setProduct(product);
        variant.setVariantName("Bản thường");
        variant.setSkuVariant(sku);
        variant.setPrice(new BigDecimal("1000000"));
        return variant;
    }

    private JsonNode register(String username) throws Exception {
        JsonNode data = data(send(post("/api/v1/auth/register"), null, Map.of(
                "email", username + "@example.com", "username", username, "password", PASSWORD,
                "fullname", "Người Dùng Chuyển Kho")).andExpect(status().isCreated()));
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
