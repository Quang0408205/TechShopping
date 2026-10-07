package com.example.Tech.controller.payment;

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
import java.time.Clock;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.notNullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Staff payment actions (/api/v1/admin/orders/{id}/payment|installment/…, /api/v1/admin/installments) over HTTP
 * against the test database (rolled back), with a real STAFF account and orders placed through the real checkout.
 * The STAFF account works at the store the orders are sent to, which has the products in stock (Phase 7.6).
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class AdminPaymentApiIntegrationTest {

    private static final String PASSWORD = "Matkhau@123";

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
    private CategoryRepository categoryRepository;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private ProductVariantRepository variantRepository;

    @Autowired
    private EntityManager entityManager;

    @Autowired
    private Clock clock;

    private final List<Long> registeredUserIds = new ArrayList<>();

    private String customerToken;
    private String staffToken;
    private ProductVariant laptop;
    private ProductVariant mouse;

    @BeforeEach
    void setUp() throws Exception {
        customerToken = register("pay.customer").get("accessToken").asString();
        staffToken = registerWithRole("pay.staff", "STAFF");

        Category category = new Category();
        category.setName("Laptop");
        category.setSlug("test-admin-payment-laptop");
        category = categoryRepository.save(category);
        Product product = productRepository.save(product("Laptop Thanh Toán", "test-admin-payment-x", category, "10000000"));
        laptop = variantRepository.save(variant(product, "16GB", "10000000"));
        mouse = variantRepository.save(variant(product, "Chuột kèm", "300000"));
        entityManager.flush();

        Store store = StoreFixtures.store(entityManager, "ZZ CN Thanh Toán", "Quận 5");
        StoreFixtures.assign(entityManager, registeredUserIds.get(1), store);
        StoreFixtures.stock(entityManager, store, laptop, 10);
        StoreFixtures.stock(entityManager, store, mouse, 10);
    }

    @AfterEach
    void revokeTokens() {
        registeredUserIds.forEach(refreshTokenService::revokeAll);
    }

    @Test
    void bankTransfer_mustBePaidBeforeConfirmation_staffConfirmsTheMoney() throws Exception {
        long orderId = placeOrder(laptop, "BANK_TRANSFER", null);

        patchStatus(orderId, "CONFIRMED")
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error.code").value("PAYMENT_REQUIRED"));
        send(post(orderUrl(orderId, "payment/confirm")), customerToken, null)
                .andExpect(status().isForbidden());
        send(post(orderUrl(orderId, "payment/confirm")), staffToken, Map.of("transactionId", "x".repeat(101)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.details.transactionId").exists());

        send(post(orderUrl(orderId, "payment/confirm")), staffToken, Map.of("transactionId", "  FT26276000123 "))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.order.payment.status").value("PAID"))
                .andExpect(jsonPath("$.data.order.payment.paidAt", notNullValue()))
                .andExpect(jsonPath("$.data.order.payment.transactionId").value("FT26276000123"))
                .andExpect(jsonPath("$.data.order.payment.bankTransfer").doesNotExist());
        send(post(orderUrl(orderId, "payment/confirm")), staffToken, null)
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error.code").value("INVALID_PAYMENT_STATUS"));
        patchStatus(orderId, "CONFIRMED").andExpect(status().isOk());
        send(get("/api/v1/orders/" + orderId), customerToken, null)
                .andExpect(jsonPath("$.data.payment.status").value("PAID"));
    }

    @Test
    void paidTransfer_cancelled_waitsForARefund_thenStaffRecordsIt() throws Exception {
        long orderId = placeOrder(laptop, "BANK_TRANSFER", null);
        send(post(orderUrl(orderId, "payment/refund")), staffToken, null)
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error.code").value("INVALID_PAYMENT_STATUS"));
        send(post(orderUrl(orderId, "payment/confirm")), staffToken, null).andExpect(status().isOk());

        send(post("/api/v1/orders/" + orderId + "/cancel"), customerToken, null)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.payment.status").value("REFUND_PENDING"));
        send(post(orderUrl(orderId, "payment/refund")), staffToken, null)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.order.payment.status").value("REFUNDED"))
                .andExpect(jsonPath("$.data.order.payment.refundedAt", notNullValue()));
    }

    @Test
    void cod_cannotBeConfirmedByHand_isPaidOnDelivery() throws Exception {
        long orderId = placeOrder(mouse, "COD", null);

        send(post(orderUrl(orderId, "payment/confirm")), staffToken, null)
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error.code").value("INVALID_PAYMENT_STATUS"));
        patchStatus(orderId, "CONFIRMED").andExpect(status().isOk());
        patchStatus(orderId, "SHIPPING").andExpect(status().isOk());
        patchStatus(orderId, "DELIVERED")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.order.payment.status").value("PAID"))
                .andExpect(jsonPath("$.data.order.payment.paidAt", notNullValue()));
    }

    @Test
    void staffCancel_ofACodOrder_cancelsThePayment() throws Exception {
        long orderId = placeOrder(mouse, "COD", null);
        patchStatus(orderId, "CONFIRMED").andExpect(status().isOk());

        patchStatus(orderId, "CANCELLED")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.order.payment.status").value("CANCELLED"));
    }

    @Test
    void installment_approval_delivery_schedule_andPeriodsInOrder_untilCompleted() throws Exception {
        long orderId = placeOrder(laptop, "INSTALLMENT", 3);
        send(post(orderUrl(orderId, "payment/confirm")), staffToken, null)
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error.code").value("PAYMENT_NOT_FOUND"));
        patchStatus(orderId, "CONFIRMED")
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error.code").value("INSTALLMENT_NOT_APPROVED"));

        send(post(orderUrl(orderId, "installment/approve")), staffToken, null)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.order.installment.status").value("APPROVED"))
                .andExpect(jsonPath("$.data.order.installment.citizenId").value("0123456789"))
                .andExpect(jsonPath("$.data.order.installment.reviewedAt", notNullValue()));
        send(post(orderUrl(orderId, "installment/approve")), staffToken, null)
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error.code").value("INVALID_INSTALLMENT_STATUS"));
        patchStatus(orderId, "CONFIRMED").andExpect(status().isOk());
        patchStatus(orderId, "SHIPPING").andExpect(status().isOk());

        JsonNode list = data(send(get("/api/v1/admin/installments").param("keyword", "DH%08d".formatted(orderId)),
                staffToken, null).andExpect(status().isOk()));
        assertThat(list.get("totalElements").asLong()).isEqualTo(1);
        long planId = list.get("content").get(0).get("installment").get("id").asLong();
        send(post(periodUrl(planId, 1)), staffToken, null)
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error.code").value("INVALID_INSTALLMENT_STATUS"));

        LocalDate today = LocalDate.now(clock);
        patchStatus(orderId, "DELIVERED")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.order.installment.status").value("ACTIVE"))
                .andExpect(jsonPath("$.data.order.installment.periods.length()").value(3))
                .andExpect(jsonPath("$.data.order.installment.periods[0].amount").value(3333333))
                .andExpect(jsonPath("$.data.order.installment.periods[0].dueDate").value(today.plusMonths(1).toString()))
                .andExpect(jsonPath("$.data.order.installment.periods[2].amount").value(3333334))
                .andExpect(jsonPath("$.data.order.installment.periods[2].dueDate").value(today.plusMonths(3).toString()));

        send(get("/api/v1/admin/installments/" + planId), staffToken, null)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.order.id").value(orderId))
                .andExpect(jsonPath("$.data.order.status").value("DELIVERED"))
                .andExpect(jsonPath("$.data.customer.username").value("pay.customer"))
                .andExpect(jsonPath("$.data.nextPeriod.number").value(1));
        send(post(periodUrl(planId, 2)), staffToken, null)
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error.code").value("INSTALLMENT_PERIOD_OUT_OF_ORDER"))
                .andExpect(jsonPath("$.error.message").value("Kỳ tiếp theo cần ghi nhận là kỳ 1"));
        send(post(periodUrl(planId, 1)), staffToken, Map.of("transactionId", "KY1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.installment.periods[0].status").value("PAID"))
                .andExpect(jsonPath("$.data.installment.periods[0].paidDate").value(today.toString()))
                .andExpect(jsonPath("$.data.installment.paidPeriods").value(1))
                .andExpect(jsonPath("$.data.installment.remainingAmount").value(6666667))
                .andExpect(jsonPath("$.data.nextPeriod.number").value(2));
        send(post(periodUrl(planId, 1)), staffToken, null)
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error.code").value("INSTALLMENT_PERIOD_OUT_OF_ORDER"));
        send(post(periodUrl(planId, 2)), staffToken, null).andExpect(status().isOk());
        send(post(periodUrl(planId, 3)), staffToken, null)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.installment.status").value("COMPLETED"))
                .andExpect(jsonPath("$.data.installment.remainingAmount").value(0))
                .andExpect(jsonPath("$.data.nextPeriod").doesNotExist());
        send(post(periodUrl(planId, 3)), staffToken, null)
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error.code").value("INVALID_INSTALLMENT_STATUS"));

        Number periodPayments = (Number) entityManager.createNativeQuery(
                        "select count(*) from payments where order_id = :id and installment_payment_id is not null "
                                + "and status = 'PAID' and payment_method = 'INSTALLMENT'")
                .setParameter("id", orderId)
                .getSingleResult();
        assertThat(periodPayments.longValue()).isEqualTo(3);
        send(get("/api/v1/orders/" + orderId), customerToken, null)
                .andExpect(jsonPath("$.data.installment.status").value("COMPLETED"))
                .andExpect(jsonPath("$.data.installment.citizenId").value("******6789"));
    }

    @Test
    void installment_rejected_withAReason_cancelsTheOrder() throws Exception {
        long orderId = placeOrder(laptop, "INSTALLMENT", 6);

        send(post(orderUrl(orderId, "installment/reject")), staffToken, Map.of("reason", "  "))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.details.reason").exists());
        send(post(orderUrl(orderId, "installment/reject")), staffToken, Map.of("reason", " Không xác minh được CCCD "))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.order.status").value("CANCELLED"))
                .andExpect(jsonPath("$.data.order.cancelledAt", notNullValue()))
                .andExpect(jsonPath("$.data.order.installment.status").value("REJECTED"))
                .andExpect(jsonPath("$.data.order.installment.rejectionReason").value("Không xác minh được CCCD"));
        send(get("/api/v1/orders/" + orderId), customerToken, null)
                .andExpect(jsonPath("$.data.status").value("CANCELLED"))
                .andExpect(jsonPath("$.data.installment.rejectionReason").value("Không xác minh được CCCD"));
        send(post(orderUrl(orderId, "installment/approve")), staffToken, null)
                .andExpect(status().isConflict());

        long codOrder = placeOrder(mouse, "COD", null);
        send(post(orderUrl(codOrder, "installment/approve")), staffToken, null)
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error.code").value("INSTALLMENT_NOT_FOUND"));
    }

    @Test
    void installmentList_filtersByStatusAndOverdue_staffOnly() throws Exception {
        long waiting = placeOrder(laptop, "INSTALLMENT", 3);
        long approved = placeOrder(laptop, "INSTALLMENT", 6);
        send(post(orderUrl(approved, "installment/approve")), staffToken, null).andExpect(status().isOk());

        send(get("/api/v1/admin/installments"), customerToken, null)
                .andExpect(status().isForbidden());
        assertOrderIds(searchInstallments("status", "PENDING_APPROVAL", "keyword", "pay.customer"), waiting);
        assertOrderIds(searchInstallments("status", "APPROVED", "keyword", "pay.customer"), approved);
        assertOrderIds(searchInstallments("keyword", "0123456789"), approved, waiting);
        assertOrderIds(searchInstallments("overdue", "true", "keyword", "pay.customer"));

        // move the schedule into the past: the first period becomes overdue
        patchStatus(approved, "CONFIRMED").andExpect(status().isOk());
        patchStatus(approved, "SHIPPING").andExpect(status().isOk());
        patchStatus(approved, "DELIVERED").andExpect(status().isOk());
        entityManager.createNativeQuery("update installment_payments set due_date = due_date - interval '2 months' "
                        + "where installment_id = (select installment_id from installment_orders where order_id = :id)")
                .setParameter("id", approved)
                .executeUpdate();
        entityManager.clear();
        JsonNode overdue = searchInstallments("overdue", "true", "keyword", "pay.customer");
        assertOrderIds(overdue, approved);
        assertThat(overdue.get("content").get(0).get("nextPeriod").get("overdue").asBoolean()).isTrue();
        send(get("/api/v1/admin/installments").param("status", "LOST"), staffToken, null)
                .andExpect(status().isBadRequest());
        send(get("/api/v1/admin/installments/999999999"), staffToken, null)
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error.code").value("INSTALLMENT_NOT_FOUND"));
    }

    @Test
    void installments_ofOrdersOutsideTheStaffMembersStore_areHiddenAndRefused() throws Exception {
        long own = placeOrder(laptop, "INSTALLMENT", 3);
        long other = placeOrder(laptop, "INSTALLMENT", 6);
        entityManager.createNativeQuery("update orders set store_id = null where order_id = :id")
                .setParameter("id", other)
                .executeUpdate();
        entityManager.clear();

        assertOrderIds(searchInstallments("keyword", "pay.customer"), own);
        Number otherPlanId = (Number) entityManager.createNativeQuery(
                        "select installment_id from installment_orders where order_id = :id")
                .setParameter("id", other)
                .getSingleResult();
        send(get("/api/v1/admin/installments/" + otherPlanId), staffToken, null)
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error.code").value("ACCESS_DENIED"));
        send(post(periodUrl(otherPlanId.longValue(), 1)), staffToken, null)
                .andExpect(status().isForbidden());
        send(post(orderUrl(other, "installment/approve")), staffToken, null)
                .andExpect(status().isForbidden());
    }

    private long placeOrder(ProductVariant variant, String method, Integer months) throws Exception {
        send(post("/api/v1/cart/items"), customerToken, Map.of("variantId", variant.getId(), "quantity", 1))
                .andExpect(status().isOk());
        Map<String, Object> body = new HashMap<>();
        body.put("recipientName", "Nguyễn Văn An");
        body.put("recipientPhone", "0901234567");
        body.put("shippingAddress", "12 Nguyễn Trãi, Quận 5, TP. Hồ Chí Minh");
        body.put("paymentMethod", method);
        if (months != null) {
            body.put("installment", Map.of("months", months, "citizenId", "0123456789", "cardBank", "VCB"));
        }
        return data(send(post("/api/v1/orders"), customerToken, body).andExpect(status().isCreated())).get("id").asLong();
    }

    private ResultActions patchStatus(long orderId, String newStatus) throws Exception {
        return send(patch("/api/v1/admin/orders/" + orderId + "/status"), staffToken, Map.of("status", newStatus));
    }

    private JsonNode searchInstallments(String... params) throws Exception {
        MockHttpServletRequestBuilder builder = get("/api/v1/admin/installments");
        for (int i = 0; i < params.length; i += 2) {
            builder.param(params[i], params[i + 1]);
        }
        return data(send(builder, staffToken, null).andExpect(status().isOk()));
    }

    private static void assertOrderIds(JsonNode page, long... expectedOrderIds) {
        List<Long> ids = new ArrayList<>();
        page.get("content").forEach(row -> ids.add(row.get("order").get("id").asLong()));
        assertThat(ids).containsExactly(java.util.Arrays.stream(expectedOrderIds).boxed().toArray(Long[]::new));
    }

    private static String orderUrl(long orderId, String action) {
        return "/api/v1/admin/orders/" + orderId + "/" + action;
    }

    private static String periodUrl(long planId, int number) {
        return "/api/v1/admin/installments/" + planId + "/periods/" + number + "/pay";
    }

    private JsonNode register(String username) throws Exception {
        JsonNode data = data(send(post("/api/v1/auth/register"), null, Map.of(
                "email", username + "@example.com", "username", username, "password", PASSWORD,
                "fullname", "Người Dùng Thanh Toán"))
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
