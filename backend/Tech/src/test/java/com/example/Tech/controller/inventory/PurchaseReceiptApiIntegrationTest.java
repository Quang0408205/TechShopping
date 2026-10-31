package com.example.Tech.controller.inventory;

import com.example.Tech.entity.inventory.InventoryId;
import com.example.Tech.entity.inventory.MovementType;
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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Suppliers and purchase receipts over HTTP against the test database (rolled back). */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class PurchaseReceiptApiIntegrationTest {

    private static final String PASSWORD = "Matkhau@123";
    private static final String SUPPLIERS = "/api/v1/admin/suppliers";

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
    private String staffToken;
    private Store storeA;
    private Store storeB;
    private ProductVariant phone;
    private ProductVariant laptop;
    private int supplierId;

    @BeforeEach
    void setUp() throws Exception {
        customerToken = register("pn.customer").get("accessToken").asString();
        adminToken = registerWithRole("pn.admin", "ADMIN");
        staffToken = registerWithRole("pn.staff", "STAFF");
        storeA = storeRepository.save(store("ZZ PN Chi nhánh A"));
        storeB = storeRepository.save(store("ZZ PN Chi nhánh B"));

        Category category = new Category();
        category.setName("Phiếu nhập test");
        category.setSlug("test-pn-category");
        category = categoryRepository.save(category);
        phone = variantRepository.save(variant(productRepository.save(product("Điện thoại PN", "test-pn-phone", category)), "TK-PN-PHONE"));
        laptop = variantRepository.save(variant(productRepository.save(product("Laptop PN", "test-pn-laptop", category)), "TK-PN-LAPTOP"));
        entityManager.flush();

        Long staffUserId = userRepository.findByUsername("pn.staff").orElseThrow().getId();
        send(post("/api/v1/admin/employees"), adminToken,
                Map.of("userId", staffUserId, "storeId", storeA.getId(), "positionAtStore", "Nhân viên bán hàng"))
                .andExpect(status().isCreated());
        supplierId = data(send(post(SUPPLIERS), adminToken, supplier("Công ty Phân phối PN"))
                .andExpect(status().isCreated())).get("id").asInt();
    }

    @AfterEach
    void revokeTokens() {
        registeredUserIds.forEach(refreshTokenService::revokeAll);
    }

    @Test
    void suppliers_staffReadOnly_adminManages_uniqueName_deleteOnlyWhenUnused() throws Exception {
        send(get(SUPPLIERS), null, null).andExpect(status().isUnauthorized());
        send(get(SUPPLIERS), customerToken, null).andExpect(status().isForbidden());
        send(get(SUPPLIERS), staffToken, null).andExpect(status().isOk());
        send(post(SUPPLIERS), staffToken, supplier("Nhà cung cấp lạ")).andExpect(status().isForbidden());
        send(put(SUPPLIERS + "/" + supplierId), staffToken, supplier("Đổi tên")).andExpect(status().isForbidden());
        send(delete(SUPPLIERS + "/" + supplierId), staffToken, null).andExpect(status().isForbidden());

        send(post(SUPPLIERS), adminToken, supplier("  công ty phân phối pn "))
                .andExpect(status().isConflict()).andExpect(jsonPath("$.error.code").value("DUPLICATE_SUPPLIER"));
        send(post(SUPPLIERS), adminToken, Map.of("name", "")).andExpect(status().isBadRequest());
        send(post(SUPPLIERS), adminToken, Map.of("name", "Sai email", "email", "khong-phai-email"))
                .andExpect(status().isBadRequest());

        assertThat(data(send(get(SUPPLIERS).param("keyword", "PHÂN phối pn"), staffToken, null).andExpect(status().isOk()))
                .get("content")).hasSize(1);
        send(put(SUPPLIERS + "/" + supplierId), adminToken, supplier("Công ty PN đổi tên"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.name").value("Công ty PN đổi tên"));

        int unused = data(send(post(SUPPLIERS), adminToken, supplier("Chưa dùng PN")).andExpect(status().isCreated())).get("id").asInt();
        send(delete(SUPPLIERS + "/" + unused), adminToken, null).andExpect(status().isOk());
        send(get(SUPPLIERS + "/" + unused), adminToken, null).andExpect(status().isNotFound());

        long receipt = createDraft(storeA, supplierId, line(phone, 1, "100"));
        send(delete(SUPPLIERS + "/" + supplierId), adminToken, null)
                .andExpect(status().isConflict()).andExpect(jsonPath("$.error.code").value("SUPPLIER_HAS_RECEIPTS"));
        send(put(SUPPLIERS + "/" + supplierId), adminToken, withActive("Công ty PN đổi tên", false)).andExpect(status().isOk());
        send(post(url(storeA)), staffToken, receiptBody(supplierId, line(phone, 1, "100")))
                .andExpect(status().isConflict()).andExpect(jsonPath("$.error.code").value("SUPPLIER_INACTIVE"));
        assertThat(receipt).isPositive();
    }

    @Test
    void draft_doesNotTouchStock_completeAddsStockLedgerAndAverageCost() throws Exception {
        long receiptId = createDraft(storeA, supplierId, line(phone, 10, "1000"), line(laptop, 4, "5000000"));
        assertThat(data(send(get(url(storeA) + "/" + receiptId), staffToken, null).andExpect(status().isOk()))
                .get("status").asString()).isEqualTo("DRAFT");
        assertThat(inventoryRepository.findById(new InventoryId(storeA.getId(), phone.getId()))).isEmpty();

        JsonNode done = data(send(post(url(storeA) + "/" + receiptId + "/complete"), staffToken, null)
                .andExpect(status().isOk()));
        assertThat(done.get("status").asString()).isEqualTo("COMPLETED");
        assertThat(done.get("code").asString()).isEqualTo("PN%06d".formatted(receiptId));
        assertThat(done.get("completedByName").asString()).isNotBlank();
        assertThat(done.get("totalQuantity").asLong()).isEqualTo(14);
        assertThat(done.get("totalCost").decimalValue()).isEqualByComparingTo("20010000");

        assertThat(inventoryRepository.findById(new InventoryId(storeA.getId(), phone.getId())).orElseThrow().getQuantity()).isEqualTo(10);
        assertThat(variantRepository.findById(phone.getId()).orElseThrow().getAverageCost()).isEqualByComparingTo("1000");
        var movements = stockMovementRepository.findByStoreIdAndVariantIdOrderByCreatedAtDescIdDesc(
                storeA.getId(), phone.getId(), org.springframework.data.domain.Pageable.unpaged()).getContent();
        assertThat(movements).hasSize(1);
        assertThat(movements.getFirst().getMovementType()).isEqualTo(MovementType.IN);
        assertThat(movements.getFirst().getReceiptId()).isEqualTo(receiptId);
        assertThat(movements.getFirst().getSupplierName()).isEqualTo("Công ty Phân phối PN");

        // a second receipt at another store: the average is weighted by the stock of every store
        long second = createDraft(storeB, supplierId, line(phone, 30, "2000"));
        send(post(url(storeB) + "/" + second + "/complete"), adminToken, null).andExpect(status().isOk());
        assertThat(variantRepository.findById(phone.getId()).orElseThrow().getAverageCost()).isEqualByComparingTo("1750");

        send(post(url(storeA) + "/" + receiptId + "/complete"), staffToken, null)
                .andExpect(status().isConflict()).andExpect(jsonPath("$.error.code").value("PURCHASE_RECEIPT_NOT_DRAFT"));
        send(post(url(storeA) + "/" + receiptId + "/cancel"), staffToken, null).andExpect(status().isConflict());
        send(put(url(storeA) + "/" + receiptId), staffToken, receiptBody(supplierId, line(phone, 1, "1")))
                .andExpect(status().isConflict());
        assertThat(inventoryRepository.findById(new InventoryId(storeA.getId(), phone.getId())).orElseThrow().getQuantity()).isEqualTo(10);
    }

    @Test
    void draft_canBeReplacedOrCancelled_andNothingIsStocked() throws Exception {
        long id = createDraft(storeA, supplierId, line(phone, 5, "100"));
        JsonNode replaced = data(send(put(url(storeA) + "/" + id), staffToken,
                receiptBody(supplierId, line(laptop, 2, "300"))).andExpect(status().isOk()));
        assertThat(replaced.get("items")).hasSize(1);
        assertThat(replaced.get("items").get(0).get("variantId").asLong()).isEqualTo(laptop.getId());

        send(post(url(storeA) + "/" + id + "/cancel"), staffToken, null)
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.status").value("CANCELLED"));
        send(post(url(storeA) + "/" + id + "/complete"), staffToken, null).andExpect(status().isConflict());
        assertThat(inventoryRepository.findById(new InventoryId(storeA.getId(), laptop.getId()))).isEmpty();
        assertThat(variantRepository.findById(laptop.getId()).orElseThrow().getAverageCost()).isNull();
    }

    @Test
    void validation_andStoreScoping() throws Exception {
        send(get(url(storeA)), null, null).andExpect(status().isUnauthorized());
        send(get(url(storeA)), customerToken, null).andExpect(status().isForbidden());
        send(get(url(storeB)), staffToken, null)
                .andExpect(status().isForbidden()).andExpect(jsonPath("$.error.code").value("ACCESS_DENIED"));
        send(post(url(storeB)), staffToken, receiptBody(supplierId, line(phone, 1, "1"))).andExpect(status().isForbidden());

        send(post(url(storeA)), staffToken, Map.of("supplierId", supplierId, "items", List.of())).andExpect(status().isBadRequest());
        send(post(url(storeA)), staffToken, receiptBody(supplierId, line(phone, 0, "10"))).andExpect(status().isBadRequest());
        send(post(url(storeA)), staffToken, receiptBody(supplierId, line(phone, 1, "-5"))).andExpect(status().isBadRequest());
        send(post(url(storeA)), staffToken, receiptBody(supplierId, line(phone, 1, "10"), line(phone, 2, "10")))
                .andExpect(status().isBadRequest());
        send(post(url(storeA)), staffToken, receiptBody(99999, line(phone, 1, "10")))
                .andExpect(status().isNotFound()).andExpect(jsonPath("$.error.code").value("SUPPLIER_NOT_FOUND"));
        send(post(url(storeA)), staffToken, receiptBody(supplierId, Map.of("variantId", 99999999L, "quantity", 1, "unitCost", 1)))
                .andExpect(status().isNotFound()).andExpect(jsonPath("$.error.code").value("PRODUCT_VARIANT_NOT_FOUND"));

        // a receipt of store B cannot be read, completed or cancelled through store A's URL
        long other = createDraftAs(adminToken, storeB, supplierId, line(phone, 1, "10"));
        send(get(url(storeA) + "/" + other), adminToken, null).andExpect(status().isNotFound());
        send(post(url(storeA) + "/" + other + "/complete"), adminToken, null).andExpect(status().isNotFound());
        send(post(url(storeB) + "/" + other + "/cancel"), staffToken, null).andExpect(status().isForbidden());
    }

    @Test
    void list_filtersByStatusAndSupplierName_newestFirst() throws Exception {
        long first = createDraft(storeA, supplierId, line(phone, 1, "10"));
        long second = createDraft(storeA, supplierId, line(laptop, 2, "20"));
        send(post(url(storeA) + "/" + first + "/complete"), staffToken, null).andExpect(status().isOk());

        JsonNode all = data(send(get(url(storeA)), staffToken, null).andExpect(status().isOk())).get("content");
        assertThat(all).hasSize(2);
        assertThat(all.get(0).get("id").asLong()).isEqualTo(second);
        assertThat(all.get(0).get("items")).isEmpty();
        assertThat(all.get(0).get("lineCount").asInt()).isEqualTo(1);
        assertThat(data(send(get(url(storeA) + "?status=COMPLETED"), staffToken, null)).get("content")).hasSize(1);
        assertThat(data(send(get(url(storeA) + "?status=DRAFT"), staffToken, null)).get("content").get(0).get("id").asLong()).isEqualTo(second);
        assertThat(data(send(get(url(storeA)).param("keyword", "Phân phối"), staffToken, null)).get("content")).hasSize(2);
        assertThat(data(send(get(url(storeA)).param("keyword", "không có"), staffToken, null)).get("content")).isEmpty();
    }

    private String url(Store store) {
        return "/api/v1/admin/stores/" + store.getId() + "/purchase-receipts";
    }

    @SafeVarargs
    private long createDraft(Store store, int supplier, Map<String, Object>... lines) throws Exception {
        return createDraftAs(staffTokenFor(store), store, supplier, lines);
    }

    @SafeVarargs
    private long createDraftAs(String token, Store store, int supplier, Map<String, Object>... lines) throws Exception {
        return data(send(post(url(store)), token, receiptBody(supplier, lines)).andExpect(status().isCreated())).get("id").asLong();
    }

    /** Staff may only act on their own store (A); everything else goes through the ADMIN. */
    private String staffTokenFor(Store store) {
        return store.getId().equals(storeA.getId()) ? staffToken : adminToken;
    }

    private Map<String, Object> line(ProductVariant variant, int quantity, String unitCost) {
        return Map.of("variantId", variant.getId(), "quantity", quantity, "unitCost", new BigDecimal(unitCost));
    }

    @SafeVarargs
    private static Map<String, Object> receiptBody(int supplier, Map<String, Object>... lines) {
        return Map.of("supplierId", supplier, "note", "Lô hàng thử", "items", List.of(lines));
    }

    private static Map<String, Object> supplier(String name) {
        return Map.of("name", name, "phone", "0901112233", "email", "ncc@mail.com", "taxCode", "0312345678");
    }

    private static Map<String, Object> withActive(String name, boolean active) {
        return Map.of("name", name, "active", active);
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
                "fullname", "Người Dùng Phiếu Nhập")).andExpect(status().isCreated()));
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
