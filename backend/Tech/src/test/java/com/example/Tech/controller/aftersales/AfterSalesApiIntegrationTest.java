package com.example.Tech.controller.aftersales;

import com.example.Tech.entity.aftersales.Warranty;
import com.example.Tech.entity.order.Order;
import com.example.Tech.entity.order.OrderItem;
import com.example.Tech.entity.order.OrderStatus;
import com.example.Tech.entity.order.PaymentMethod;
import com.example.Tech.entity.product.Category;
import com.example.Tech.entity.product.Product;
import com.example.Tech.entity.product.ProductVariant;
import com.example.Tech.repository.aftersales.WarrantyRepository;
import com.example.Tech.repository.order.OrderRepository;
import com.example.Tech.repository.product.CategoryRepository;
import com.example.Tech.repository.product.ProductRepository;
import com.example.Tech.repository.product.ProductVariantRepository;
import com.example.Tech.repository.user.UserRepository;
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
import org.springframework.mock.web.MockMultipartFile;
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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Customer after-sales API over HTTP against the test database (rolled back). */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class AfterSalesApiIntegrationTest {

    private static final String PASSWORD = "Matkhau@123";
    private static final byte[] JPEG = Arrays.copyOf(new byte[]{(byte) 0xFF, (byte) 0xD8, (byte) 0xFF, (byte) 0xE0}, 64);
    private static final String ISSUE = "Máy tự tắt nguồn khi đang sạc pin.";

    @Autowired private MockMvc mockMvc;
    @Autowired private JsonMapper jsonMapper;
    @Autowired private RefreshTokenService refreshTokenService;
    @Autowired private UserRepository userRepository;
    @Autowired private CategoryRepository categoryRepository;
    @Autowired private ProductRepository productRepository;
    @Autowired private ProductVariantRepository variantRepository;
    @Autowired private OrderRepository orderRepository;
    @Autowired private WarrantyRepository warrantyRepository;
    @Autowired private EntityManager entityManager;
    @Autowired private Clock clock;

    private final List<Long> registeredUserIds = new ArrayList<>();

    private String tokenA;
    private String tokenB;
    private ProductVariant phone;
    private ProductVariant cable;
    private Order delivered;
    private OrderItem phoneLine;
    private OrderItem cableLine;
    private Order oldOrder;
    private Order installmentOrder;
    private Order pendingOrder;

    @BeforeEach
    void setUp() throws Exception {
        tokenA = register("as.khacha", "Khách Hậu Mãi A");
        tokenB = register("as.khachb", "Khách Hậu Mãi B");

        Category category = new Category();
        category.setName("Hậu mãi test");
        category.setSlug("test-as-category");
        category = categoryRepository.save(category);
        phone = variant(category, "Điện thoại AS", "test-as-phone", 12, "10000000");
        cable = variant(category, "Cáp sạc AS", "test-as-cable", 0, "200000");

        LocalDateTime now = LocalDateTime.now(clock);
        delivered = order(OrderStatus.DELIVERED, PaymentMethod.COD, now.minusDays(2));
        phoneLine = line(delivered, phone, 1, "10000000");
        cableLine = line(delivered, cable, 3, "200000");
        delivered = orderRepository.save(delivered);
        warranty(phoneLine, now.minusDays(2));

        oldOrder = order(OrderStatus.DELIVERED, PaymentMethod.COD, now.minusDays(8));
        line(oldOrder, phone, 1, "10000000");
        oldOrder = orderRepository.save(oldOrder);
        installmentOrder = order(OrderStatus.DELIVERED, PaymentMethod.INSTALLMENT, now.minusDays(1));
        line(installmentOrder, phone, 1, "10000000");
        installmentOrder = orderRepository.save(installmentOrder);
        pendingOrder = order(OrderStatus.PENDING, PaymentMethod.COD, null);
        line(pendingOrder, phone, 1, "10000000");
        pendingOrder = orderRepository.save(pendingOrder);
        entityManager.flush();
    }

    @AfterEach
    void revokeTokens() {
        registeredUserIds.forEach(refreshTokenService::revokeAll);
    }

    @Test
    void everythingNeedsAnAccount() throws Exception {
        send(get(afterSalesUrl(delivered)), null, null).andExpect(status().isUnauthorized());
        send(post("/api/v1/warranty-requests"), null, warrantyBody(phoneLine, ISSUE, null)).andExpect(status().isUnauthorized());
        send(get("/api/v1/service-requests/mine"), null, null).andExpect(status().isUnauthorized());
        mockMvc.perform(multipart("/api/v1/uploads/service-images").file(photoPart())).andExpect(status().isUnauthorized());
    }

    @Test
    void afterSalesView_warrantyPerLine_returnWindow_installmentAndOtherAccounts() throws Exception {
        JsonNode view = data(send(get(afterSalesUrl(delivered)), tokenA, null).andExpect(status().isOk()));
        assertThat(view.get("orderCode").asString()).isEqualTo("DH%08d".formatted(delivered.getId()));
        assertThat(view.get("returnAvailable").asBoolean()).isTrue();
        assertThat(LocalDateTime.parse(view.get("returnDeadline").asString()))
                .isEqualTo(delivered.getDeliveredAt().plusDays(7));
        JsonNode phoneItem = view.get("items").get(0);
        assertThat(phoneItem.get("productName").asString()).isEqualTo("Điện thoại AS");
        assertThat(phoneItem.get("warrantyValid").asBoolean()).isTrue();
        assertThat(phoneItem.get("returnableQuantity").asInt()).isEqualTo(1);
        JsonNode cableItem = view.get("items").get(1);
        assertThat(cableItem.get("warrantyValid").asBoolean()).isFalse();
        assertThat(cableItem.get("warrantyEndDate").isNull()).isTrue();
        assertThat(cableItem.get("returnableQuantity").asInt()).isEqualTo(3);

        JsonNode installment = data(send(get(afterSalesUrl(installmentOrder)), tokenA, null));
        assertThat(installment.get("returnAvailable").asBoolean()).isFalse();
        assertThat(installment.get("returnUnavailableReason").asString()).contains("trả góp");
        assertThat(installment.get("items").get(0).get("returnableQuantity").asInt()).isZero();
        assertThat(data(send(get(afterSalesUrl(oldOrder)), tokenA, null)).get("returnUnavailableReason").asString())
                .contains("7 ngày");

        send(get(afterSalesUrl(delivered)), tokenB, null).andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error.code").value("ORDER_NOT_FOUND"));
        send(get(afterSalesUrl(pendingOrder)), tokenA, null).andExpect(status().isConflict())
                .andExpect(jsonPath("$.error.code").value("AFTER_SALES_NOT_AVAILABLE"));
    }

    @Test
    void warrantyRequest_withAPhoto_oneOpenPerLine_cancelThenAgain() throws Exception {
        String photo = upload(tokenA);
        assertThat(photo).contains("/uploads/service/");
        JsonNode created = data(send(post("/api/v1/warranty-requests"), tokenA,
                warrantyBody(phoneLine, "  " + ISSUE + "  ", List.of(photo))).andExpect(status().isCreated()));
        long id = created.get("id").asLong();
        assertThat(created.get("code").asString()).isEqualTo("BH%06d".formatted(id));
        assertThat(created.get("type").asString()).isEqualTo("WARRANTY");
        assertThat(created.get("status").asString()).isEqualTo("PENDING");
        assertThat(created.get("cancellable").asBoolean()).isTrue();
        assertThat(created.get("description").asString()).isEqualTo(ISSUE);
        assertThat(created.get("imageUrls").get(0).asString()).isEqualTo(photo);
        assertThat(created.get("warrantyEndDate").isNull()).isFalse();
        assertThat(created.get("items").get(0).get("orderItemId").asLong()).isEqualTo(phoneLine.getId());
        assertThat(created.get("customerName").asString()).isEqualTo("Khách Hậu Mãi A");

        send(post("/api/v1/warranty-requests"), tokenA, warrantyBody(phoneLine, ISSUE, null))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error.code").value("SERVICE_REQUEST_ALREADY_OPEN"));
        assertThat(data(send(get(afterSalesUrl(delivered)), tokenA, null)).get("items").get(0)
                .get("openWarrantyRequestId").asLong()).isEqualTo(id);

        send(post(serviceUrl("WARRANTY", id) + "/cancel"), tokenB, null).andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error.code").value("SERVICE_REQUEST_NOT_FOUND"));
        send(get(serviceUrl("WARRANTY", id)), tokenB, null).andExpect(status().isNotFound());
        send(post(serviceUrl("WARRANTY", id) + "/cancel"), tokenA, null).andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("CANCELLED"))
                .andExpect(jsonPath("$.data.cancellable").value(false))
                .andExpect(jsonPath("$.data.cancelledAt").exists());
        send(post(serviceUrl("WARRANTY", id) + "/cancel"), tokenA, null).andExpect(status().isConflict())
                .andExpect(jsonPath("$.error.code").value("INVALID_SERVICE_REQUEST_STATUS"));
        send(post("/api/v1/warranty-requests"), tokenA, warrantyBody(phoneLine, ISSUE, null)).andExpect(status().isCreated());
    }

    @Test
    void warrantyRequest_refusals() throws Exception {
        send(post("/api/v1/warranty-requests"), tokenA, warrantyBody(cableLine, ISSUE, null))
                .andExpect(status().isConflict()).andExpect(jsonPath("$.error.code").value("WARRANTY_NOT_VALID"));
        send(post("/api/v1/warranty-requests"), tokenB, warrantyBody(phoneLine, ISSUE, null))
                .andExpect(status().isNotFound()).andExpect(jsonPath("$.error.code").value("ORDER_NOT_FOUND"));
        send(post("/api/v1/warranty-requests"), tokenA, warrantyBody(pendingOrder.getItems().getFirst(), ISSUE, null))
                .andExpect(status().isConflict()).andExpect(jsonPath("$.error.code").value("AFTER_SALES_NOT_AVAILABLE"));
        send(post("/api/v1/warranty-requests"), tokenA, warrantyBody(phoneLine, "Hỏng", null))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.error.details.description").exists());
        send(post("/api/v1/warranty-requests"), tokenA,
                warrantyBody(phoneLine, ISSUE, List.of("https://example.com/anh.jpg")))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.error.details.imageUrls").exists());
        String reviewPhoto = jsonMapper.readTree(mockMvc.perform(multipart("/api/v1/uploads/review-images")
                        .file(photoPart()).header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenA))
                .andReturn().getResponse().getContentAsString()).get("data").get("url").asString();
        send(post("/api/v1/warranty-requests"), tokenA, warrantyBody(phoneLine, ISSUE, List.of(reviewPhoto)))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.error.details.imageUrls").exists());
    }

    @Test
    void maintenanceRequest_withoutWarranty_oneOpenPerLine() throws Exception {
        JsonNode created = data(send(post("/api/v1/maintenance-requests"), tokenA,
                Map.of("orderItemId", cableLine.getId(), "maintenanceType", "CLEANING",
                        "description", "Vệ sinh đầu cáp bị bụi bẩn.")).andExpect(status().isCreated()));
        assertThat(created.get("code").asString()).startsWith("BT");
        assertThat(created.get("maintenanceType").asString()).isEqualTo("CLEANING");
        assertThat(created.get("estimatedCost").isNull()).isTrue();

        send(post("/api/v1/maintenance-requests"), tokenA, Map.of("orderItemId", cableLine.getId(),
                "maintenanceType", "REPAIR", "description", "Thử gửi yêu cầu thứ hai.")).andExpect(status().isConflict())
                .andExpect(jsonPath("$.error.code").value("SERVICE_REQUEST_ALREADY_OPEN"));
        send(post("/api/v1/maintenance-requests"), tokenA, Map.of("orderItemId", phoneLine.getId(),
                "maintenanceType", "PAINT", "description", "Loại bảo trì không có.")).andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("MALFORMED_REQUEST"));
        send(post("/api/v1/maintenance-requests"), tokenA, Map.of("orderItemId", phoneLine.getId(),
                "description", "Thiếu loại bảo trì.")).andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.details.maintenanceType").exists());
    }

    @Test
    void returnRequest_refundFromPaidPrices_heldQuantities_cancelFreesThem() throws Exception {
        JsonNode first = data(send(post("/api/v1/return-requests"), tokenA, returnBody(delivered,
                List.of(item(phoneLine, 1), item(cableLine, 2)))).andExpect(status().isCreated()));
        assertThat(first.get("code").asString()).startsWith("DT");
        assertThat(first.get("refundAmount").decimalValue()).isEqualByComparingTo("10400000");
        assertThat(first.get("reasonType").asString()).isEqualTo("DEFECTIVE");
        assertThat(first.get("items")).hasSize(2);
        assertThat(first.get("items").get(1).get("quantity").asInt()).isEqualTo(2);
        assertThat(first.get("items").get(1).get("refundAmount").decimalValue()).isEqualByComparingTo("400000");

        send(post("/api/v1/return-requests"), tokenA, returnBody(delivered, List.of(item(cableLine, 2))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.details.items").value("Chỉ còn 1 sản phẩm \"Cáp sạc AS\" có thể trả"));
        send(post("/api/v1/return-requests"), tokenA, returnBody(delivered, List.of(item(cableLine, 1))))
                .andExpect(status().isCreated());
        JsonNode view = data(send(get(afterSalesUrl(delivered)), tokenA, null));
        assertThat(view.get("items").get(0).get("returnableQuantity").asInt()).isZero();
        assertThat(view.get("items").get(1).get("returnableQuantity").asInt()).isZero();

        send(post(serviceUrl("RETURN", first.get("id").asLong()) + "/cancel"), tokenA, null).andExpect(status().isOk());
        view = data(send(get(afterSalesUrl(delivered)), tokenA, null));
        assertThat(view.get("items").get(0).get("returnableQuantity").asInt()).isEqualTo(1);
        assertThat(view.get("items").get(1).get("returnableQuantity").asInt()).isEqualTo(2);
    }

    @Test
    void returnRequest_refusals() throws Exception {
        send(post("/api/v1/return-requests"), tokenA, returnBody(delivered, List.of(item(cableLine, 1), item(cableLine, 1))))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.error.details.items").exists());
        send(post("/api/v1/return-requests"), tokenA,
                returnBody(delivered, List.of(item(oldOrder.getItems().getFirst(), 1))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.details.items").value("Sản phẩm không thuộc đơn hàng này"));
        send(post("/api/v1/return-requests"), tokenA, returnBody(delivered, List.of(item(cableLine, 0))))
                .andExpect(status().isBadRequest());
        send(post("/api/v1/return-requests"), tokenA, returnBody(delivered, List.of()))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.error.details.items").exists());
        send(post("/api/v1/return-requests"), tokenA,
                returnBody(installmentOrder, List.of(item(installmentOrder.getItems().getFirst(), 1))))
                .andExpect(status().isConflict()).andExpect(jsonPath("$.error.code").value("RETURN_NOT_AVAILABLE"))
                .andExpect(jsonPath("$.error.message").value("Đơn trả góp không trả hàng qua website; vui lòng liên hệ cửa hàng"));
        send(post("/api/v1/return-requests"), tokenA, returnBody(oldOrder, List.of(item(oldOrder.getItems().getFirst(), 1))))
                .andExpect(status().isConflict()).andExpect(jsonPath("$.error.code").value("RETURN_NOT_AVAILABLE"));
        send(post("/api/v1/return-requests"), tokenA,
                returnBody(pendingOrder, List.of(item(pendingOrder.getItems().getFirst(), 1))))
                .andExpect(status().isConflict()).andExpect(jsonPath("$.error.code").value("AFTER_SALES_NOT_AVAILABLE"));
        send(post("/api/v1/return-requests"), tokenB, returnBody(delivered, List.of(item(cableLine, 1))))
                .andExpect(status().isNotFound()).andExpect(jsonPath("$.error.code").value("ORDER_NOT_FOUND"));
    }

    @Test
    void mine_threeTypes_newestFirst_filters_andOnlyMyOwn() throws Exception {
        send(post("/api/v1/warranty-requests"), tokenA, warrantyBody(phoneLine, ISSUE, null)).andExpect(status().isCreated());
        send(post("/api/v1/maintenance-requests"), tokenA, Map.of("orderItemId", cableLine.getId(),
                "maintenanceType", "SOFTWARE", "description", "Cài lại phần mềm điều khiển.")).andExpect(status().isCreated());
        long returnId = data(send(post("/api/v1/return-requests"), tokenA,
                returnBody(delivered, List.of(item(cableLine, 1))))).get("id").asLong();
        send(post(serviceUrl("RETURN", returnId) + "/cancel"), tokenA, null).andExpect(status().isOk());

        JsonNode page = data(send(get("/api/v1/service-requests/mine"), tokenA, null).andExpect(status().isOk()));
        assertThat(page.get("totalElements").asLong()).isEqualTo(3);
        assertThat(types(page)).containsExactly("RETURN", "MAINTENANCE", "WARRANTY");
        assertThat(page.get("content").get(0).get("orderCode").asString()).isEqualTo("DH%08d".formatted(delivered.getId()));
        assertThat(types(data(send(get("/api/v1/service-requests/mine?type=MAINTENANCE"), tokenA, null))))
                .containsExactly("MAINTENANCE");
        assertThat(types(data(send(get("/api/v1/service-requests/mine?status=cancelled"), tokenA, null))))
                .containsExactly("RETURN");
        assertThat(data(send(get("/api/v1/service-requests/mine?size=2&page=1"), tokenA, null)).get("content")).hasSize(1);
        assertThat(data(send(get("/api/v1/service-requests/mine"), tokenB, null)).get("totalElements").asLong()).isZero();

        send(get(serviceUrl("RETURN", returnId)), tokenA, null).andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("CANCELLED"));
        send(get("/api/v1/service-requests/REPAIR/1"), tokenA, null).andExpect(status().isBadRequest());
    }

    private ProductVariant variant(Category category, String name, String slug, int warrantyMonths, String price) {
        Product product = new Product();
        product.setName(name);
        product.setSlug(slug);
        product.setCategory(category);
        product.setBasePrice(new BigDecimal(price));
        product.setWarrantyMonths(warrantyMonths);
        product = productRepository.save(product);
        ProductVariant variant = new ProductVariant();
        variant.setProduct(product);
        variant.setVariantName("Mặc định");
        variant.setPrice(new BigDecimal(price));
        return variantRepository.save(variant);
    }

    private Order order(OrderStatus status, PaymentMethod paymentMethod, LocalDateTime deliveredAt) {
        Order order = new Order();
        order.setUser(userRepository.findByUsername("as.khacha").orElseThrow());
        order.setRecipientName("Khách Hậu Mãi A");
        order.setRecipientPhone("0901234567");
        order.setShippingAddress("1 Lê Lợi, Quận 1, Hồ Chí Minh");
        order.setTotalAmount(new BigDecimal("10600000"));
        order.setStatus(status);
        order.setPaymentMethod(paymentMethod);
        order.setDeliveredAt(deliveredAt);
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

    private void warranty(OrderItem line, LocalDateTime deliveredAt) {
        Warranty warranty = new Warranty();
        warranty.setOrderItem(line);
        warranty.setStartDate(deliveredAt.toLocalDate());
        warranty.setEndDate(deliveredAt.toLocalDate().plusMonths(12));
        warrantyRepository.save(warranty);
    }

    private static Map<String, Object> warrantyBody(OrderItem line, String description, List<String> imageUrls) {
        Map<String, Object> body = new HashMap<>();
        body.put("orderItemId", line.getId());
        body.put("description", description);
        body.put("imageUrls", imageUrls);
        return body;
    }

    private static Map<String, Object> returnBody(Order order, List<Map<String, Object>> items) {
        return Map.of("orderId", order.getId(), "reasonType", "DEFECTIVE",
                "reason", "Màn hình có điểm chết ngay khi mở hộp.", "items", items);
    }

    private static Map<String, Object> item(OrderItem line, int quantity) {
        return Map.of("orderItemId", line.getId(), "quantity", quantity);
    }

    private static List<String> types(JsonNode page) {
        List<String> types = new ArrayList<>();
        page.get("content").forEach(row -> types.add(row.get("type").asString()));
        return types;
    }

    private static String afterSalesUrl(Order order) {
        return "/api/v1/orders/" + order.getId() + "/after-sales";
    }

    private static String serviceUrl(String type, long id) {
        return "/api/v1/service-requests/" + type + "/" + id;
    }

    private static MockMultipartFile photoPart() {
        return new MockMultipartFile("file", "loi.jpg", "image/jpeg", JPEG);
    }

    private String upload(String token) throws Exception {
        return data(mockMvc.perform(multipart("/api/v1/uploads/service-images").file(photoPart())
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isCreated())).get("url").asString();
    }

    private String register(String username, String fullname) throws Exception {
        JsonNode data = data(send(post("/api/v1/auth/register"), null, Map.of(
                "email", username + "@example.com", "username", username, "password", PASSWORD,
                "fullname", fullname))
                .andExpect(status().isCreated()));
        registeredUserIds.add(data.get("user").get("id").asLong());
        return data.get("accessToken").asString();
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
