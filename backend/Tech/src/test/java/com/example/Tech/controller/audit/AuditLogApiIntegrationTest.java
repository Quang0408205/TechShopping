package com.example.Tech.controller.audit;

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
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** What the audit filter records, and who may read it, over HTTP against the test database (rolled back). */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class AuditLogApiIntegrationTest {

    private static final String PASSWORD = "Matkhau@123";
    private static final String LOGS = "/api/v1/admin/audit-logs";

    @Autowired private MockMvc mockMvc;
    @Autowired private JsonMapper jsonMapper;
    @Autowired private RefreshTokenService refreshTokenService;
    @Autowired private AuditLogRepository auditLogRepository;
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
    private String staffToken;
    private Long adminId;
    private Store store;
    private ProductVariant variant;

    @BeforeEach
    void setUp() throws Exception {
        customerToken = register("au.customer").get("accessToken").asString();
        adminToken = registerWithRole("au.admin", "ADMIN");
        adminId = userRepository.findByUsername("au.admin").orElseThrow().getId();
        staffToken = registerWithRole("au.staff", "STAFF");
        store = storeRepository.save(store("ZZ AU Chi nhánh"));
        Category category = new Category();
        category.setName("Nhật ký test");
        category.setSlug("test-au-category");
        category = categoryRepository.save(category);
        Product product = new Product();
        product.setName("Điện thoại AU");
        product.setSlug("test-au-phone");
        product.setCategory(category);
        product.setBasePrice(new BigDecimal("1000000"));
        variant = new ProductVariant();
        variant.setProduct(productRepository.save(product));
        variant.setVariantName("Bản thường");
        variant.setSkuVariant("TK-AU-PHONE");
        variant.setPrice(new BigDecimal("1000000"));
        variant = variantRepository.save(variant);
        entityManager.flush();
        Long staffUserId = userRepository.findByUsername("au.staff").orElseThrow().getId();
        send(post("/api/v1/admin/employees"), adminToken,
                Map.of("userId", staffUserId, "storeId", store.getId(), "positionAtStore", "Nhân viên bán hàng"))
                .andExpect(status().isCreated());
        auditLogRepository.deleteAll();
        entityManager.flush();
    }

    @AfterEach
    void revokeTokens() {
        registeredUserIds.forEach(refreshTokenService::revokeAll);
    }

    @Test
    void anAdminWrite_leavesOneLine_withWhoWhatAndMaskedData() throws Exception {
        int id = data(send(post("/api/v1/admin/suppliers"), adminToken, Map.of("name", "NCC nhật ký", "phone", "0901112233"))
                .andExpect(status().isCreated())).get("id").asInt();
        send(put("/api/v1/admin/suppliers/" + id), adminToken, Map.of("name", "NCC nhật ký đổi tên")).andExpect(status().isOk());
        send(delete("/api/v1/admin/suppliers/" + id), adminToken, null).andExpect(status().isOk());

        List<AuditLog> logs = auditLogRepository.findAll().stream().sorted((a, b) -> a.getId().compareTo(b.getId())).toList();
        assertThat(logs).extracting(AuditLog::getActionLabel)
                .containsExactly("Tạo · Nhà cung cấp", "Cập nhật · Nhà cung cấp", "Xoá · Nhà cung cấp");
        AuditLog created = logs.get(0);
        assertThat(created.getUserId()).isEqualTo(adminId);
        assertThat(created.getUsername()).isEqualTo("au.admin");
        assertThat(created.getFullName()).isEqualTo("Người Dùng Nhật Ký");
        assertThat(created.getRole()).isEqualTo("ADMIN");
        assertThat(created.getMethod()).isEqualTo("POST");
        assertThat(created.getPath()).isEqualTo("/api/v1/admin/suppliers");
        assertThat(created.getEntityType()).isEqualTo("suppliers");
        assertThat(created.getEntityId()).isNull();
        assertThat(created.getStatus()).isEqualTo(201);
        assertThat(created.getIp()).isNotBlank();
        assertThat(created.getCreatedAt()).isNotNull();
        assertThat(created.getRequestData()).contains("NCC nhật ký").contains("0901112233");
        assertThat(logs.get(1).getEntityId()).isEqualTo(String.valueOf(id));
        assertThat(logs.get(2).getEntityId()).isEqualTo(String.valueOf(id));
    }

    @Test
    void passwordsAreNeverStored() throws Exception {
        send(put("/api/v1/users/me/password"), adminToken, Map.of("currentPassword", PASSWORD, "newPassword", "MatkhauMoi@456"))
                .andExpect(status().isOk());
        AuditLog line = auditLogRepository.findAll().getFirst();
        assertThat(line.getActionLabel()).isEqualTo("Đổi mật khẩu · Người dùng");
        assertThat(line.getRequestData()).contains("\"currentPassword\":\"***\"").contains("\"newPassword\":\"***\"")
                .doesNotContain(PASSWORD).doesNotContain("MatkhauMoi@456");
    }

    @Test
    void staffWritesAreLoggedWithTheirRole_refusedAndFailedRequestsAndReadsAreNot() throws Exception {
        send(post("/api/v1/admin/stores/" + store.getId() + "/inventory/stock-in"), staffToken,
                Map.of("variantId", variant.getId(), "quantity", 3, "note", "Nhập thử")).andExpect(status().isOk());
        AuditLog line = auditLogRepository.findAll().getFirst();
        assertThat(line.getRole()).isEqualTo("STAFF");
        assertThat(line.getUsername()).isEqualTo("au.staff");
        assertThat(line.getActionLabel()).isEqualTo("Nhập kho · Tồn kho");
        assertThat(line.getEntityType()).isEqualTo("inventory");

        long before = auditLogRepository.count();
        send(post("/api/v1/admin/suppliers"), adminToken, Map.of("name", "NCC trùng")).andExpect(status().isCreated());
        send(post("/api/v1/admin/suppliers"), adminToken, Map.of("name", "ncc TRÙNG")).andExpect(status().isConflict());
        send(post("/api/v1/admin/suppliers"), adminToken, Map.of("name", "")).andExpect(status().isBadRequest());
        send(post("/api/v1/admin/suppliers"), staffToken, Map.of("name", "NCC của nhân viên")).andExpect(status().isForbidden());
        send(post("/api/v1/admin/suppliers"), customerToken, Map.of("name", "NCC của khách")).andExpect(status().isForbidden());
        send(post("/api/v1/admin/suppliers"), null, Map.of("name", "NCC ẩn danh")).andExpect(status().isUnauthorized());
        send(get("/api/v1/admin/suppliers"), adminToken, null).andExpect(status().isOk());
        send(get(LOGS), adminToken, null).andExpect(status().isOk());
        assertThat(auditLogRepository.count()).isEqualTo(before + 1);
    }

    @Test
    void theTrailIsReadOnlyAndAdminOnly_andFiltersNarrowIt() throws Exception {
        send(get(LOGS), null, null).andExpect(status().isUnauthorized());
        send(get(LOGS), customerToken, null).andExpect(status().isForbidden());
        send(get(LOGS), staffToken, null).andExpect(status().isForbidden());
        send(get(LOGS + "/entity-types"), staffToken, null).andExpect(status().isForbidden());
        send(post(LOGS), adminToken, Map.of()).andExpect(status().isMethodNotAllowed());
        send(delete(LOGS + "/1"), adminToken, null).andExpect(status().is4xxClientError());

        send(post("/api/v1/admin/suppliers"), adminToken, Map.of("name", "NCC lọc một")).andExpect(status().isCreated());
        int id = data(send(post("/api/v1/admin/suppliers"), adminToken, Map.of("name", "NCC lọc hai")).andExpect(status().isCreated())).get("id").asInt();
        send(post("/api/v1/admin/stores/" + store.getId() + "/inventory/stock-in"), staffToken,
                Map.of("variantId", variant.getId(), "quantity", 1)).andExpect(status().isOk());
        send(delete("/api/v1/admin/suppliers/" + id), adminToken, null).andExpect(status().isOk());

        JsonNode all = page("");
        assertThat(all.get("totalElements").asInt()).isEqualTo(4);
        assertThat(all.get("content").get(0).get("actionLabel").asString()).isEqualTo("Xoá · Nhà cung cấp");
        assertThat(all.get("content").get(0).get("entityLabel").asString()).isEqualTo("Nhà cung cấp");
        assertThat(page("?entityType=suppliers").get("totalElements").asInt()).isEqualTo(3);
        assertThat(page("?entityType=inventory").get("totalElements").asInt()).isEqualTo(1);
        assertThat(page("?method=DELETE").get("totalElements").asInt()).isEqualTo(1);
        assertThat(page("?method=post&entityType=suppliers").get("totalElements").asInt()).isEqualTo(2);
        assertThat(page("?keyword=au.staff").get("totalElements").asInt()).isEqualTo(1);
        assertThat(page("?keyword=NGƯỜI DÙNG NHẬT KÝ").get("totalElements").asInt()).isEqualTo(4);
        assertThat(page("?keyword=không-có-ai").get("totalElements").asInt()).isZero();
        assertThat(page("?userId=" + adminId).get("totalElements").asInt()).isEqualTo(3);
        assertThat(page("?fromDate=" + LocalDate.now() + "&toDate=" + LocalDate.now()).get("totalElements").asInt()).isEqualTo(4);
        assertThat(page("?toDate=" + LocalDate.now().minusDays(1)).get("totalElements").asInt()).isZero();
        assertThat(page("?fromDate=" + LocalDate.now().plusDays(1)).get("totalElements").asInt()).isZero();
        assertThat(page("?size=2&page=1").get("content")).hasSize(2);

        JsonNode types = data(send(get(LOGS + "/entity-types"), adminToken, null).andExpect(status().isOk()));
        assertThat(types.toString()).contains("\"suppliers\"").contains("Nhà cung cấp").contains("\"inventory\"").contains("Tồn kho");
    }

    private JsonNode page(String query) throws Exception {
        return data(send(get(LOGS + query), adminToken, null).andExpect(status().isOk()));
    }

    private static Store store(String name) {
        Store store = new Store();
        store.setName(name);
        store.setAddress("1 Đường Test");
        store.setDistrict("Quận 1");
        store.setCity("Hồ Chí Minh");
        return store;
    }

    private JsonNode register(String username) throws Exception {
        JsonNode data = data(send(post("/api/v1/auth/register"), null, Map.of(
                "email", username + "@example.com", "username", username, "password", PASSWORD,
                "fullname", "Người Dùng Nhật Ký")).andExpect(status().isCreated()));
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
