package com.example.Tech.controller.aftersales;

import com.example.Tech.entity.aftersales.Warranty;
import com.example.Tech.entity.order.Order;
import com.example.Tech.entity.order.OrderItem;
import com.example.Tech.entity.order.OrderStatus;
import com.example.Tech.entity.order.PaymentMethod;
import com.example.Tech.entity.product.Category;
import com.example.Tech.entity.product.Product;
import com.example.Tech.entity.product.ProductVariant;
import com.example.Tech.entity.store.Store;
import com.example.Tech.entity.user.UserRole;
import com.example.Tech.repository.aftersales.WarrantyRepository;
import com.example.Tech.repository.order.OrderRepository;
import com.example.Tech.repository.product.CategoryRepository;
import com.example.Tech.repository.product.ProductRepository;
import com.example.Tech.repository.product.ProductVariantRepository;
import com.example.Tech.repository.user.CustomerProfileRepository;
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
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Staff side of after-sales requests over HTTP against the test database (rolled back). */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class AdminServiceRequestApiIntegrationTest {

    private static final String PASSWORD = "Matkhau@123";
    private static final String ADMIN_URL = "/api/v1/admin/service-requests";

    @Autowired private MockMvc mockMvc;
    @Autowired private JsonMapper jsonMapper;
    @Autowired private RefreshTokenService refreshTokenService;
    @Autowired private UserRepository userRepository;
    @Autowired private RoleRepository roleRepository;
    @Autowired private UserRoleRepository userRoleRepository;
    @Autowired private CustomerProfileRepository customerProfileRepository;
    @Autowired private CategoryRepository categoryRepository;
    @Autowired private ProductRepository productRepository;
    @Autowired private ProductVariantRepository variantRepository;
    @Autowired private OrderRepository orderRepository;
    @Autowired private WarrantyRepository warrantyRepository;
    @Autowired private EntityManager entityManager;

    private final List<Long> registeredUserIds = new ArrayList<>();

    private Long customerId;
    private String customerToken;
    private String staffAToken;
    private String staffBToken;
    private String adminToken;
    private Store storeA;
    private ProductVariant phone;
    private ProductVariant cover;
    private Order orderA;
    private Long phoneLineId;
    private Long coverLineId;
    private Long orderBLineId;

    @BeforeEach
    void setUp() throws Exception {
        JsonNode customer = register("asa.khach", "Khách Hậu Mãi");
        customerId = customer.get("user").get("id").asLong();
        customerToken = customer.get("accessToken").asString();
        staffAToken = registerWithRole("asa.nva", "Nhân Viên A", "STAFF");
        Long staffAId = registeredUserIds.getLast();
        staffBToken = registerWithRole("asa.nvb", "Nhân Viên B", "STAFF");
        Long staffBId = registeredUserIds.getLast();
        adminToken = registerWithRole("asa.admin", "Quản Trị", "ADMIN");

        Category category = new Category();
        category.setName("Hậu mãi quản trị");
        category.setSlug("test-asa-category");
        category = categoryRepository.save(category);
        phone = variant(category, "Điện thoại ASA", "test-asa-phone", "10000000");
        cover = variant(category, "Ốp lưng ASA", "test-asa-cover", "200000");
        storeA = StoreFixtures.store(entityManager, "ZZ CN Hậu Mãi A", "Quận 5");
        Store storeB = StoreFixtures.store(entityManager, "ZZ CN Hậu Mãi B", "Quận 9");
        StoreFixtures.assign(entityManager, staffAId, storeA);
        StoreFixtures.assign(entityManager, staffBId, storeB);
        StoreFixtures.stock(entityManager, storeA, phone, 5);

        orderA = order(storeA, "0907777777");
        OrderItem phoneLine = line(orderA, phone, 1, "10000000");
        OrderItem coverLine = line(orderA, cover, 2, "200000");
        orderA = orderRepository.save(orderA);
        warranty(phoneLine);
        Order orderB = order(storeB, "0908888888");
        OrderItem orderBLine = line(orderB, cover, 1, "200000");
        orderRepository.save(orderB);
        entityManager.flush();
        phoneLineId = phoneLine.getId();
        coverLineId = coverLine.getId();
        orderBLineId = orderBLine.getId();
        // as if both orders had been delivered through the order flow
        customerProfileRepository.addToTotalSpent(customerId, new BigDecimal("10800000"));
    }

    @AfterEach
    void revokeTokens() {
        registeredUserIds.forEach(refreshTokenService::revokeAll);
    }

    @Test
    void access_byRoleAndStore() throws Exception {
        long id = createWarranty();
        send(get(ADMIN_URL), null, null).andExpect(status().isUnauthorized());
        send(get(ADMIN_URL), customerToken, null).andExpect(status().isForbidden());
        send(patch(url("WARRANTY", id)), customerToken, Map.of("status", "RECEIVED")).andExpect(status().isForbidden());

        assertThat(data(send(get(ADMIN_URL), staffBToken, null)).get("totalElements").asLong()).isZero();
        // a STAFF member's storeId filter is ignored: always their own store
        assertThat(data(send(get(ADMIN_URL + "?storeId=" + storeA.getId()), staffBToken, null))
                .get("totalElements").asLong()).isZero();
        send(get(url("WARRANTY", id)), staffBToken, null).andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error.code").value("ACCESS_DENIED"));
        send(patch(url("WARRANTY", id)), staffBToken, Map.of("status", "RECEIVED")).andExpect(status().isForbidden());

        assertThat(data(send(get(ADMIN_URL), staffAToken, null)).get("totalElements").asLong()).isEqualTo(1);
        send(get(url("WARRANTY", id)), staffAToken, null).andExpect(status().isOk())
                .andExpect(jsonPath("$.data.storeName").value("ZZ CN Hậu Mãi A"))
                .andExpect(jsonPath("$.data.recipientPhone").value("0907777777"))
                .andExpect(jsonPath("$.data.customerEmail").value("asa.khach@example.com"));
        send(get(url("WARRANTY", -1)), adminToken, null).andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error.code").value("SERVICE_REQUEST_NOT_FOUND"));
    }

    @Test
    void warrantyFlow_receiveProcessComplete_customerSeesIt() throws Exception {
        long id = createWarranty();
        send(patch(url("WARRANTY", id)), staffAToken, Map.of("status", "PROCESSING")).andExpect(status().isConflict())
                .andExpect(jsonPath("$.error.code").value("INVALID_SERVICE_REQUEST_STATUS"));
        send(patch(url("WARRANTY", id)), staffAToken, Map.of("status", "RECEIVED", "estimatedCost", 1000))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.error.details.estimatedCost").exists());
        send(patch(url("WARRANTY", id)), staffAToken, Map.of("status", "SOMETHING")).andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.details.status").value("Trạng thái không hợp lệ"));

        JsonNode received = data(send(patch(url("WARRANTY", id)), staffAToken, Map.of("status", "RECEIVED",
                "estimatedCompletionDate", "2026-12-01", "notes", "Đã nhận máy, kiểm tra pin.")).andExpect(status().isOk()));
        assertThat(received.get("status").asString()).isEqualTo("RECEIVED");
        assertThat(received.get("handlerName").asString()).isEqualTo("Nhân Viên A");
        assertThat(received.get("receivedAt").isNull()).isFalse();
        assertThat(received.get("cancellable").asBoolean()).isFalse();
        send(post("/api/v1/service-requests/WARRANTY/" + id + "/cancel"), customerToken, null)
                .andExpect(status().isConflict());

        send(patch(url("WARRANTY", id)), adminToken, Map.of("notes", "Ghi chú của quản trị.")).andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error.code").value("ADMIN_READ_ONLY"));
        send(patch(url("WARRANTY", id)), staffAToken, Map.of("notes", "Thay pin mới.")).andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("RECEIVED"))
                .andExpect(jsonPath("$.data.handlerName").value("Nhân Viên A"));
        send(patch(url("WARRANTY", id)), staffAToken, Map.of("status", "PROCESSING")).andExpect(status().isOk());
        send(patch(url("WARRANTY", id)), staffAToken, Map.of("status", "COMPLETED")).andExpect(status().isOk())
                .andExpect(jsonPath("$.data.completedAt").exists());
        send(patch(url("WARRANTY", id)), staffAToken, Map.of("notes", "Sửa sau khi xong")).andExpect(status().isConflict());

        send(get("/api/v1/service-requests/WARRANTY/" + id), customerToken, null).andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("COMPLETED"))
                .andExpect(jsonPath("$.data.notes").value("Thay pin mới."))
                .andExpect(jsonPath("$.data.estimatedCompletionDate").value("2026-12-01"));
        // the line is free for a new warranty request
        send(post("/api/v1/warranty-requests"), customerToken, Map.of("orderItemId", phoneLineId,
                "description", "Lại bị tắt nguồn sau khi sửa.")).andExpect(status().isCreated());
    }

    @Test
    void maintenance_costs_rejectNeedsAReason() throws Exception {
        long rejected = createMaintenance(coverLineId);
        send(patch(url("MAINTENANCE", rejected)), staffAToken, Map.of("status", "REJECTED"))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.error.details.rejectionReason").exists());
        send(patch(url("MAINTENANCE", rejected)), staffAToken, Map.of("status", "REJECTED",
                "rejectionReason", " Sản phẩm không thuộc diện bảo trì tại cửa hàng ")).andExpect(status().isOk());
        send(get("/api/v1/service-requests/MAINTENANCE/" + rejected), customerToken, null)
                .andExpect(jsonPath("$.data.status").value("REJECTED"))
                .andExpect(jsonPath("$.data.rejectionReason").value("Sản phẩm không thuộc diện bảo trì tại cửa hàng"));

        long done = createMaintenance(phoneLineId);
        send(patch(url("MAINTENANCE", done)), staffAToken, Map.of("status", "RECEIVED", "estimatedCost", 300000))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.estimatedCost").value(300000));
        send(patch(url("MAINTENANCE", done)), staffAToken, Map.of("estimatedCost", -1)).andExpect(status().isBadRequest());
        send(patch(url("MAINTENANCE", done)), staffAToken, Map.of("status", "PROCESSING")).andExpect(status().isOk());
        send(patch(url("MAINTENANCE", done)), staffAToken, Map.of("status", "COMPLETED", "actualCost", 250000))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.actualCost").value(250000))
                .andExpect(jsonPath("$.data.completedAt").exists());
    }

    @Test
    void returnFlow_approveReceiveWithRestock_refundTakesOffTotalSpent() throws Exception {
        long id = data(send(post("/api/v1/return-requests"), customerToken, Map.of("orderId", orderA.getId(),
                "reasonType", "DEFECTIVE", "reason", "Màn hình bị sọc ngay khi mở hộp.",
                "items", List.of(Map.of("orderItemId", phoneLineId, "quantity", 1),
                        Map.of("orderItemId", coverLineId, "quantity", 2)))).andExpect(status().isCreated())).get("id").asLong();

        send(patch(url("RETURN", id)), staffAToken, Map.of()).andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.details.status").exists());
        send(patch(url("RETURN", id)), staffAToken, Map.of("status", "RECEIVED")).andExpect(status().isConflict());
        send(patch(url("RETURN", id)), staffAToken, Map.of("status", "APPROVED", "notes", "x"))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.error.details.notes").exists());
        send(patch(url("RETURN", id)), staffAToken, Map.of("status", "APPROVED")).andExpect(status().isOk())
                .andExpect(jsonPath("$.data.approvedAt").exists())
                .andExpect(jsonPath("$.data.handlerName").value("Nhân Viên A"));
        send(patch(url("RETURN", id)), staffAToken, Map.of("status", "RECEIVED",
                "restockOrderItemIds", List.of(phoneLineId, orderBLineId))).andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.details.restockOrderItemIds").exists());

        JsonNode received = data(send(patch(url("RETURN", id)), staffAToken, Map.of("status", "RECEIVED",
                "restockOrderItemIds", List.of(phoneLineId))).andExpect(status().isOk()));
        assertThat(received.get("items").get(0).get("restocked").asBoolean()).isTrue();
        assertThat(received.get("items").get(1).get("restocked").asBoolean()).isFalse();
        assertThat(StoreFixtures.quantity(entityManager, storeA, phone)).isEqualTo(6);
        assertThat(StoreFixtures.quantity(entityManager, storeA, cover)).isZero();
        assertThat(StoreFixtures.movements(entityManager, orderA.getId())).containsExactly("RETURN:1");

        BigDecimal spentBefore = customerProfileRepository.findById(customerId).orElseThrow().getTotalSpent();
        send(patch(url("RETURN", id)), staffAToken, Map.of("status", "REFUNDED")).andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("REFUNDED"))
                .andExpect(jsonPath("$.data.completedAt").exists());
        assertThat(customerProfileRepository.findById(customerId).orElseThrow().getTotalSpent())
                .isEqualByComparingTo(spentBefore.subtract(new BigDecimal("10400000")));
        send(patch(url("RETURN", id)), staffAToken, Map.of("status", "REJECTED", "rejectionReason", "Muộn"))
                .andExpect(status().isConflict());
    }

    @Test
    void returnRejected_freesTheQuantity_theAdminOnlyViews() throws Exception {
        long id = data(send(post("/api/v1/return-requests"), customerToken, Map.of("orderId", orderA.getId(),
                "reasonType", "CHANGED_MIND", "reason", "Không hợp màu, muốn trả lại.",
                "items", List.of(Map.of("orderItemId", coverLineId, "quantity", 2))))).get("id").asLong();
        send(patch(url("RETURN", id)), adminToken, Map.of("status", "APPROVED")).andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error.code").value("ADMIN_READ_ONLY"));
        send(get(url("RETURN", id)), adminToken, null).andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("PENDING"));
        send(patch(url("RETURN", id)), staffAToken, Map.of("status", "APPROVED")).andExpect(status().isOk());
        send(patch(url("RETURN", id)), staffAToken, Map.of("status", "REJECTED",
                "rejectionReason", "Hộp đã bị rách, không đủ điều kiện trả.")).andExpect(status().isOk())
                .andExpect(jsonPath("$.data.handlerName").value("Nhân Viên A"));
        JsonNode view = data(send(get("/api/v1/orders/" + orderA.getId() + "/after-sales"), customerToken, null));
        assertThat(view.get("items").get(1).get("returnableQuantity").asInt()).isEqualTo(2);
    }

    @Test
    void search_filters() throws Exception {
        long warrantyId = createWarranty();
        createMaintenance(coverLineId);
        send(post("/api/v1/maintenance-requests"), customerToken, Map.of("orderItemId", orderBLineId,
                "maintenanceType", "OTHER", "description", "Đơn ở chi nhánh B cần bảo trì.")).andExpect(status().isCreated());

        assertThat(data(send(get(ADMIN_URL), adminToken, null)).get("totalElements").asLong()).isGreaterThanOrEqualTo(3);
        assertThat(codes(ADMIN_URL + "?storeId=" + storeA.getId(), adminToken)).hasSize(2);
        assertThat(codes(ADMIN_URL + "?type=MAINTENANCE", staffAToken)).hasSize(1);
        assertThat(codes(ADMIN_URL + "?status=pending", staffAToken)).hasSize(2);
        assertThat(codes(ADMIN_URL + "?keyword=BH" + "%06d".formatted(warrantyId), staffAToken))
                .containsExactly("BH%06d".formatted(warrantyId));
        assertThat(codes(ADMIN_URL + "?keyword=DH%08d".formatted(orderA.getId()), staffAToken)).hasSize(2);
        assertThat(codes(ADMIN_URL + "?keyword=asa.khach@", staffAToken)).hasSize(2);
        assertThat(codes(ADMIN_URL + "?keyword=0907777", staffAToken)).hasSize(2);
        assertThat(codes(ADMIN_URL + "?keyword=khong-co-ai", staffAToken)).isEmpty();
        assertThat(codes(ADMIN_URL + "?fromDate=" + LocalDateTime.now().toLocalDate().plusDays(1), staffAToken)).isEmpty();
        send(get(ADMIN_URL + "?fromDate=2026-10-10&toDate=2026-10-01"), staffAToken, null)
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.error.details.fromDate").exists());
    }

    private long createWarranty() throws Exception {
        return data(send(post("/api/v1/warranty-requests"), customerToken, Map.of("orderItemId", phoneLineId,
                "description", "Máy tự tắt nguồn khi đang sạc pin.")).andExpect(status().isCreated())).get("id").asLong();
    }

    private long createMaintenance(Long lineId) throws Exception {
        return data(send(post("/api/v1/maintenance-requests"), customerToken, Map.of("orderItemId", lineId,
                "maintenanceType", "CLEANING", "description", "Vệ sinh toàn bộ sản phẩm.")).andExpect(status().isCreated()))
                .get("id").asLong();
    }

    private List<String> codes(String url, String token) throws Exception {
        List<String> codes = new ArrayList<>();
        data(send(get(url), token, null).andExpect(status().isOk())).get("content")
                .forEach(row -> codes.add(row.get("code").asString()));
        return codes;
    }

    private ProductVariant variant(Category category, String name, String slug, String price) {
        Product product = new Product();
        product.setName(name);
        product.setSlug(slug);
        product.setCategory(category);
        product.setBasePrice(new BigDecimal(price));
        product = productRepository.save(product);
        ProductVariant variant = new ProductVariant();
        variant.setProduct(product);
        variant.setVariantName("Mặc định");
        variant.setPrice(new BigDecimal(price));
        return variantRepository.save(variant);
    }

    private Order order(Store store, String recipientPhone) {
        Order order = new Order();
        order.setUser(userRepository.getReferenceById(customerId));
        order.setRecipientName("Khách Hậu Mãi");
        order.setRecipientPhone(recipientPhone);
        order.setShippingAddress("1 Lê Lợi, Quận 5, Hồ Chí Minh");
        order.setTotalAmount(new BigDecimal("10400000"));
        order.setStatus(OrderStatus.DELIVERED);
        order.setPaymentMethod(PaymentMethod.COD);
        order.setDeliveredAt(LocalDateTime.now().minusDays(1));
        order.setStore(store);
        return order;
    }

    private static OrderItem line(Order order, ProductVariant variant, int quantity, String unitPrice) {
        OrderItem item = new OrderItem();
        item.setVariant(variant);
        item.setQuantity(quantity);
        item.setUnitPrice(new BigDecimal(unitPrice));
        item.setSubtotal(new BigDecimal(unitPrice).multiply(BigDecimal.valueOf(quantity)));
        order.addItem(item);
        return item;
    }

    private void warranty(OrderItem line) {
        Warranty warranty = new Warranty();
        warranty.setOrderItem(line);
        warranty.setStartDate(LocalDateTime.now().toLocalDate().minusDays(1));
        warranty.setEndDate(LocalDateTime.now().toLocalDate().plusMonths(12));
        warrantyRepository.save(warranty);
    }

    private static String url(String type, long id) {
        return ADMIN_URL + "/" + type + "/" + id;
    }

    private JsonNode register(String username, String fullname) throws Exception {
        JsonNode data = data(send(post("/api/v1/auth/register"), null, Map.of(
                "email", username + "@example.com", "username", username, "password", PASSWORD,
                "fullname", fullname)).andExpect(status().isCreated()));
        registeredUserIds.add(data.get("user").get("id").asLong());
        return data;
    }

    private String registerWithRole(String username, String fullname, String role) throws Exception {
        Long id = register(username, fullname).get("user").get("id").asLong();
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
