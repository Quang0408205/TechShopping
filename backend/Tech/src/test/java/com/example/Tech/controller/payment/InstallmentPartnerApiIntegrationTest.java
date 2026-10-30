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
import com.example.Tech.service.payment.InstallmentDecisionService;
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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * The installment partners over HTTP against the test database (rolled back): the partner webhook, the answer of the
 * simulated partner and the ADMIN fee table. Orders are placed through the real checkout.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class InstallmentPartnerApiIntegrationTest {

    private static final String PASSWORD = "Matkhau@123";
    private static final String WEBHOOK_SECRET = "test-only-webhook-secret";
    private static final String WEBHOOK_URL = "/api/v1/payments/installment/webhook";

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
    private InstallmentDecisionService decisions;

    @Autowired
    private EntityManager entityManager;

    private final List<Long> registeredUserIds = new ArrayList<>();

    private String customerToken;
    private String otherCustomerToken;
    private String staffToken;
    private String adminToken;
    private ProductVariant laptop;

    @BeforeEach
    void setUp() throws Exception {
        customerToken = register("partner.customer").get("accessToken").asString();
        otherCustomerToken = register("partner.other").get("accessToken").asString();
        staffToken = registerWithRole("partner.staff", "STAFF");
        adminToken = registerWithRole("partner.admin", "ADMIN");

        Category category = new Category();
        category.setName("Laptop");
        category.setSlug("test-partner-laptop");
        category = categoryRepository.save(category);
        Product product = new Product();
        product.setName("Laptop Đối Tác");
        product.setSlug("test-partner-x");
        product.setCategory(category);
        product.setBasePrice(new BigDecimal("10000000"));
        product = productRepository.save(product);
        ProductVariant variant = new ProductVariant();
        variant.setProduct(product);
        variant.setVariantName("16GB");
        variant.setPrice(new BigDecimal("10000000"));
        laptop = variantRepository.save(variant);
        entityManager.flush();

        Store store = StoreFixtures.store(entityManager, "ZZ CN Đối Tác", "Quận 7");
        StoreFixtures.assign(entityManager, registeredUserIds.get(2), store);
        StoreFixtures.stock(entityManager, store, laptop, 10);
    }

    @AfterEach
    void revokeTokens() {
        registeredUserIds.forEach(refreshTokenService::revokeAll);
    }

    @Test
    void webhook_needsTheSharedSecret() throws Exception {
        placeInstallment("CREDIT_CARD", 3, "0123456789", customerToken);
        Map<String, Object> body = Map.of("eventId", "evt-1", "partnerRef", "CC-NONE", "decision", "APPROVED");

        webhook(body, null).andExpect(status().isUnauthorized()).andExpect(jsonPath("$.error.code").value("WEBHOOK_UNAUTHORIZED"));
        webhook(body, "wrong-secret").andExpect(status().isUnauthorized());
        webhook(body, WEBHOOK_SECRET).andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error.code").value("INSTALLMENT_NOT_FOUND"));
        send(post(WEBHOOK_URL), customerToken, body).andExpect(status().isUnauthorized());
    }

    @Test
    void webhook_validatesTheBody() throws Exception {
        webhook(Map.of("partnerRef", "CC-1", "decision", "APPROVED"), WEBHOOK_SECRET).andExpect(status().isBadRequest());
        webhook(Map.of("eventId", "e", "partnerRef", "CC-1", "decision", "MAYBE"), WEBHOOK_SECRET)
                .andExpect(status().isBadRequest());
    }

    @Test
    void webhook_approval_paysTheOrder_andARepeatedEventChangesNothing() throws Exception {
        long orderId = placeInstallment("FINANCE_A", 9, "0123456789", customerToken);
        String partnerRef = partnerRefOf(orderId);

        webhook(Map.of("eventId", "evt-7", "partnerRef", partnerRef, "decision", "APPROVED"), WEBHOOK_SECRET)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.applied").value(true));
        send(get("/api/v1/orders/" + orderId), customerToken, null)
                .andExpect(jsonPath("$.data.installment.status").value("APPROVED"))
                .andExpect(jsonPath("$.data.payment.status").value("PAID"));
        // 5% of 10.000.000đ for company A over 9 months
        send(get("/api/v1/admin/installments").param("keyword", "partner.customer"), staffToken, null)
                .andExpect(jsonPath("$.data.content[0].installment.feeAmount").value(500000))
                .andExpect(jsonPath("$.data.content[0].installment.netAmount").value(9500000));

        webhook(Map.of("eventId", "evt-7", "partnerRef", partnerRef, "decision", "APPROVED"), WEBHOOK_SECRET)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.applied").value(false));
        webhook(Map.of("eventId", "evt-8", "partnerRef", partnerRef, "decision", "REJECTED", "reason", "late"), WEBHOOK_SECRET)
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error.code").value("INVALID_INSTALLMENT_STATUS"));
    }

    @Test
    void webhook_rejection_keepsTheOrderPending_withTheReason() throws Exception {
        long orderId = placeInstallment("CREDIT_CARD", 6, "0123456789", customerToken);

        webhook(Map.of("eventId", "evt-1", "partnerRef", partnerRefOf(orderId), "decision", "REJECTED",
                "reason", "Thẻ hết hạn mức"), WEBHOOK_SECRET)
                .andExpect(status().isOk());
        send(get("/api/v1/orders/" + orderId), customerToken, null)
                .andExpect(jsonPath("$.data.status").value("PENDING"))
                .andExpect(jsonPath("$.data.installment.status").value("REJECTED"))
                .andExpect(jsonPath("$.data.installment.rejectionReason").value("Thẻ hết hạn mức"))
                .andExpect(jsonPath("$.data.payment.status").value("PENDING"));
    }

    @Test
    void simulatedPartner_answersByTheLastDigitOfTheCitizenId() throws Exception {
        long approved = placeInstallment("CREDIT_CARD", 3, "0123456781", customerToken);
        long refusedByCard = placeInstallment("CREDIT_CARD", 3, "0123456780", customerToken);
        long refusedByA = placeInstallment("FINANCE_A", 6, "0123456785", customerToken);
        long acceptedByB = placeInstallment("FINANCE_B", 6, "0123456785", customerToken);
        long refusedByAll = placeInstallment("FINANCE_B", 6, "0123456789", customerToken);

        for (long orderId : List.of(approved, refusedByCard, refusedByA, acceptedByB, refusedByAll)) {
            decisions.decideAsPartner(orderId);
        }
        entityManager.flush();
        entityManager.clear();

        assertStatus(approved, "APPROVED");
        assertStatus(refusedByCard, "REJECTED");
        assertStatus(refusedByA, "REJECTED");
        assertStatus(acceptedByB, "APPROVED");
        assertStatus(refusedByAll, "REJECTED");
        // answering twice changes nothing
        decisions.decideAsPartner(approved);
        assertStatus(approved, "APPROVED");
    }

    @Test
    void resubmit_isRefusedForAnotherCustomersOrder() throws Exception {
        long orderId = placeInstallment("CREDIT_CARD", 3, "0123456780", customerToken);
        decisions.decideAsPartner(orderId);
        Map<String, Object> request = Map.of("months", 6, "citizenId", "0123456781", "partner", "FINANCE_A");

        send(post("/api/v1/orders/" + orderId + "/installment/resubmit"), otherCustomerToken, request)
                .andExpect(status().isNotFound());
        send(post("/api/v1/orders/" + orderId + "/installment/resubmit"), null, request)
                .andExpect(status().isUnauthorized());
        send(post("/api/v1/orders/" + orderId + "/installment/resubmit"), customerToken, request)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.installment.partner").value("FINANCE_A"));
    }

    @Test
    void feeTable_isAdminOnly_andLaterApprovalsUseTheNewFee_whileApprovedOnesKeepTheirs() throws Exception {
        long before = placeInstallment("CREDIT_CARD", 3, "0123456781", customerToken);
        send(post("/api/v1/admin/orders/" + before + "/installment/approve"), staffToken, null).andExpect(status().isOk());
        long after = placeInstallment("CREDIT_CARD", 3, "0123456781", customerToken);

        send(get("/api/v1/admin/installment-partners"), staffToken, null).andExpect(status().isForbidden());
        send(get("/api/v1/admin/installment-partners"), customerToken, null).andExpect(status().isForbidden());
        send(get("/api/v1/admin/installment-partners"), adminToken, null)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(3))
                .andExpect(jsonPath("$.data[0].code").value("CREDIT_CARD"))
                .andExpect(jsonPath("$.data[0].rates.length()").value(4))
                .andExpect(jsonPath("$.data[0].rates[0].numMonths").value(3))
                .andExpect(jsonPath("$.data[0].rates[0].feePercent").value(2));

        Map<String, Object> update = new HashMap<>();
        update.put("active", true);
        update.put("minOrderTotal", 3000000);
        update.put("maxOrderTotal", null);
        update.put("rates", List.of(Map.of("numMonths", 3, "feePercent", 2.5), Map.of("numMonths", 6, "feePercent", 3)));
        send(put("/api/v1/admin/installment-partners/CREDIT_CARD"), staffToken, update).andExpect(status().isForbidden());
        send(put("/api/v1/admin/installment-partners/CREDIT_CARD"), adminToken, update)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.rates.length()").value(2))
                .andExpect(jsonPath("$.data.rates[0].feePercent").value(2.5));

        send(post("/api/v1/admin/orders/" + after + "/installment/approve"), staffToken, null)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.order.installment.feeAmount").value(250000))
                .andExpect(jsonPath("$.data.order.installment.netAmount").value(9750000));
        send(get("/api/v1/admin/installments/" + installmentIdOf(before)), staffToken, null)
                .andExpect(jsonPath("$.data.installment.feeAmount").value(200000));

        // a term the partner no longer has cannot be chosen
        Map<String, Object> gone = new HashMap<>();
        gone.put("months", 12);
        gone.put("citizenId", "0123456781");
        gone.put("cardBank", "VCB");
        gone.put("partner", "CREDIT_CARD");
        addToCart();
        send(post("/api/v1/orders"), customerToken, orderBody(gone)).andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.details['installment.months']").exists());
    }

    @Test
    void feeTable_validatesTheUpdate_andAPausedPartnerIsNotOffered() throws Exception {
        Map<String, Object> bad = new HashMap<>();
        bad.put("active", true);
        bad.put("minOrderTotal", 5000000);
        bad.put("maxOrderTotal", 4000000);
        bad.put("rates", List.of(Map.of("numMonths", 3, "feePercent", 2)));
        send(put("/api/v1/admin/installment-partners/FINANCE_A"), adminToken, bad)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.details.maxOrderTotal").exists());
        bad.put("maxOrderTotal", null);
        bad.put("rates", List.of(Map.of("numMonths", 3, "feePercent", 2), Map.of("numMonths", 3, "feePercent", 3)));
        send(put("/api/v1/admin/installment-partners/FINANCE_A"), adminToken, bad)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.details.rates").exists());
        bad.put("rates", List.of(Map.of("numMonths", 3, "feePercent", 101)));
        send(put("/api/v1/admin/installment-partners/FINANCE_A"), adminToken, bad).andExpect(status().isBadRequest());
        bad.put("rates", List.of());
        send(put("/api/v1/admin/installment-partners/FINANCE_A"), adminToken, bad).andExpect(status().isBadRequest());
        send(put("/api/v1/admin/installment-partners/NO_SUCH"), adminToken, Map.of("active", true, "minOrderTotal", 1,
                "rates", List.of(Map.of("numMonths", 3, "feePercent", 2)))).andExpect(status().isNotFound());

        send(put("/api/v1/admin/installment-partners/FINANCE_A"), adminToken, Map.of("active", false, "minOrderTotal", 3000000,
                "rates", List.of(Map.of("numMonths", 6, "feePercent", 4))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.active").value(false));
        send(get("/api/v1/payments/installment-options"), customerToken, null)
                .andExpect(jsonPath("$.data.partners.length()").value(2));
        addToCart();
        send(post("/api/v1/orders"), customerToken, orderBody(new HashMap<>(Map.of("months", 6, "citizenId", "0123456781",
                "partner", "FINANCE_A"))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.details['installment.partner']").exists());
    }

    private void assertStatus(long orderId, String expected) throws Exception {
        send(get("/api/v1/orders/" + orderId), customerToken, null)
                .andExpect(jsonPath("$.data.installment.status").value(expected));
    }

    private String partnerRefOf(long orderId) {
        return (String) entityManager.createNativeQuery("select partner_ref from installment_orders where order_id = :id")
                .setParameter("id", orderId)
                .getSingleResult();
    }

    private long installmentIdOf(long orderId) {
        return ((Number) entityManager.createNativeQuery("select installment_id from installment_orders where order_id = :id")
                .setParameter("id", orderId)
                .getSingleResult()).longValue();
    }

    private ResultActions webhook(Map<String, Object> body, String secret) throws Exception {
        MockHttpServletRequestBuilder builder = post(WEBHOOK_URL);
        if (secret != null) {
            builder.header("X-Webhook-Secret", secret);
        }
        return send(builder, null, body);
    }

    private void addToCart() throws Exception {
        send(post("/api/v1/cart/items"), customerToken, Map.of("variantId", laptop.getId(), "quantity", 1))
                .andExpect(status().isOk());
    }

    private Map<String, Object> orderBody(Map<String, Object> installment) {
        Map<String, Object> body = new HashMap<>();
        body.put("recipientName", "Nguyễn Văn An");
        body.put("recipientPhone", "0901234567");
        body.put("shippingAddress", "12 Nguyễn Trãi, Quận 5, TP. Hồ Chí Minh");
        body.put("paymentMethod", "INSTALLMENT");
        body.put("installment", installment);
        return body;
    }

    private long placeInstallment(String partner, int months, String citizenId, String token) throws Exception {
        send(post("/api/v1/cart/items"), token, Map.of("variantId", laptop.getId(), "quantity", 1))
                .andExpect(status().isOk());
        Map<String, Object> installment = new HashMap<>();
        installment.put("months", months);
        installment.put("citizenId", citizenId);
        installment.put("partner", partner);
        if (partner.equals("CREDIT_CARD")) {
            installment.put("cardBank", "VCB");
        }
        return data(send(post("/api/v1/orders"), token, orderBody(installment)).andExpect(status().isCreated()))
                .get("id").asLong();
    }

    private JsonNode register(String username) throws Exception {
        JsonNode data = data(send(post("/api/v1/auth/register"), null, Map.of(
                "email", username + "@example.com", "username", username, "password", PASSWORD,
                "fullname", "Người Dùng Đối Tác"))
                .andExpect(status().isCreated()));
        registeredUserIds.add(data.get("user").get("id").asLong());
        return data;
    }

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
