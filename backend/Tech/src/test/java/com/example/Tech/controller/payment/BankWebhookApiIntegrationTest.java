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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * The bank webhook (POST /api/v1/payments/webhook) and the ADMIN reconciliation
 * (/api/v1/admin/bank-transactions) over HTTP against the test database (rolled back), with orders placed through
 * the real checkout. The webhook key and the simulator come from application-test.yml.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class BankWebhookApiIntegrationTest {

    private static final String PASSWORD = "Matkhau@123";
    private static final String SECRET = "test-only-webhook-secret";
    private static final String PRICE = "10000000";

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

    private final List<Long> registeredUserIds = new ArrayList<>();

    private String customerToken;
    private String staffToken;
    private String adminToken;
    private ProductVariant laptop;
    private int sequence;

    @BeforeEach
    void setUp() throws Exception {
        customerToken = register("bw.customer").get("accessToken").asString();
        staffToken = registerWithRole("bw.staff", "STAFF");
        adminToken = registerWithRole("bw.admin", "ADMIN");

        Category category = new Category();
        category.setName("Laptop");
        category.setSlug("test-bank-webhook-laptop");
        category = categoryRepository.save(category);
        Product product = new Product();
        product.setName("Laptop Webhook");
        product.setSlug("test-bank-webhook-x");
        product.setCategory(category);
        product.setBasePrice(new BigDecimal(PRICE));
        product = productRepository.save(product);
        ProductVariant variant = new ProductVariant();
        variant.setProduct(product);
        variant.setVariantName("16GB");
        variant.setPrice(new BigDecimal(PRICE));
        laptop = variantRepository.save(variant);
        entityManager.flush();

        Store store = StoreFixtures.store(entityManager, "ZZ CN Webhook", "Quận 5");
        StoreFixtures.assign(entityManager, registeredUserIds.get(1), store);
        StoreFixtures.stock(entityManager, store, laptop, 20);
    }

    @AfterEach
    void revokeTokens() {
        registeredUserIds.forEach(refreshTokenService::revokeAll);
    }

    @Test
    void webhook_needsTheSharedKey() throws Exception {
        Map<String, Object> body = notice("KEY-1", "DH00000001 test", PRICE);

        webhook(body, null).andExpect(status().isUnauthorized()).andExpect(jsonPath("$.error.code").value("WEBHOOK_UNAUTHORIZED"));
        webhook(body, "wrong-key").andExpect(status().isUnauthorized());
        // a user token is not a webhook key
        mockMvc.perform(post("/api/v1/payments/webhook").header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON).content(jsonMapper.writeValueAsString(body)))
                .andExpect(status().isUnauthorized());
        // both header styles are accepted
        webhook(body, SECRET).andExpect(status().isOk());
        mockMvc.perform(post("/api/v1/payments/webhook").header("X-Webhook-Secret", SECRET)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonMapper.writeValueAsString(notice("KEY-2", "khong ma don", PRICE))))
                .andExpect(status().isOk());
    }

    @Test
    void webhook_validatesTheBody() throws Exception {
        Map<String, Object> noAmount = notice("BAD-1", "DH00000001", PRICE);
        noAmount.remove("transferAmount");
        webhook(noAmount, SECRET).andExpect(status().isBadRequest()).andExpect(jsonPath("$.error.details.transferAmount").exists());
        webhook(notice("BAD-2", "DH00000001", "-5"), SECRET).andExpect(status().isBadRequest());
        Map<String, Object> noId = notice("x", "DH00000001", PRICE);
        noId.remove("id");
        webhook(noId, SECRET).andExpect(status().isBadRequest());
    }

    @Test
    void exactTransfer_confirmsTheOrderAutomatically() throws Exception {
        long orderId = placeOrder("BANK_TRANSFER");
        patchStatus(orderId, "CONFIRMED").andExpect(status().isConflict())
                .andExpect(jsonPath("$.error.code").value("PAYMENT_REQUIRED"));

        webhook(notice("FT-OK-1", "Nguyen Van An chuyen tien " + code(orderId), PRICE), SECRET)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("MATCHED"))
                .andExpect(jsonPath("$.data.orderCode").value(code(orderId)))
                .andExpect(jsonPath("$.data.duplicate").value(false));

        patchStatus(orderId, "CONFIRMED").andExpect(status().isOk());
        JsonNode payment = data(send(get("/api/v1/orders/" + orderId), customerToken, null).andExpect(status().isOk())).get("payment");
        assertThat(payment.get("status").asString()).isEqualTo("PAID");
        assertThat(payment.get("transactionId").asString()).startsWith("FT-OK-1");
    }

    @Test
    void orderCode_isFoundWhateverTheBankDoesToIt() throws Exception {
        long orderId = placeOrder("BANK_TRANSFER");
        String lower = code(orderId).toLowerCase().replace("dh", "dh ");

        webhook(notice("FT-LOWER-1", "chuyen tien " + lower + " cam on", PRICE), SECRET)
                .andExpect(jsonPath("$.data.status").value("MATCHED"));
    }

    @Test
    void sameTransactionId_isStoredOnce() throws Exception {
        long orderId = placeOrder("BANK_TRANSFER");
        Map<String, Object> body = notice("FT-DUP-1", code(orderId), PRICE);

        webhook(body, SECRET).andExpect(jsonPath("$.data.duplicate").value(false)).andExpect(jsonPath("$.data.status").value("MATCHED"));
        webhook(body, SECRET).andExpect(status().isOk()).andExpect(jsonPath("$.data.duplicate").value(true));

        assertThat(list("status", "MATCHED").get("content")).hasSize(1);
        assertThat(list("status", "REVIEW").get("content")).isEmpty();
    }

    @Test
    void moneyOut_isIgnored() throws Exception {
        long orderId = placeOrder("BANK_TRANSFER");
        Map<String, Object> out = notice("FT-OUT-1", code(orderId), PRICE);
        out.put("transferType", "out");

        webhook(out, SECRET).andExpect(status().isOk()).andExpect(jsonPath("$.data.status").doesNotExist());

        patchStatus(orderId, "CONFIRMED").andExpect(status().isConflict());
        assertThat(list().get("content")).isEmpty();
    }

    @Test
    void wrongAmount_waitsForTheAdmin_whoCanApplyItToTheOrder() throws Exception {
        long orderId = placeOrder("BANK_TRANSFER");

        webhook(notice("FT-LESS-1", code(orderId), "9000000"), SECRET)
                .andExpect(jsonPath("$.data.status").value("REVIEW"))
                .andExpect(jsonPath("$.data.orderCode").value(code(orderId)));
        patchStatus(orderId, "CONFIRMED").andExpect(status().isConflict());

        JsonNode attention = list("attention", "true").get("content");
        assertThat(attention).hasSize(1);
        assertThat(attention.get(0).get("reason").asString()).contains("khác số tiền");
        long txId = attention.get(0).get("id").asLong();
        assertThat(data(send(get("/api/v1/admin/bank-transactions/info"), adminToken, null).andExpect(status().isOk()))
                .get("needsAttention").asLong()).isGreaterThanOrEqualTo(1);

        send(post("/api/v1/admin/bank-transactions/" + txId + "/assign"), adminToken, Map.of("orderId", orderId, "note", "Khách chuyển thiếu, đã gọi xác nhận"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("RESOLVED"))
                .andExpect(jsonPath("$.data.orderCode").value(code(orderId)));
        patchStatus(orderId, "CONFIRMED").andExpect(status().isOk());
        assertThat(list("attention", "true").get("content")).isEmpty();

        send(post("/api/v1/admin/bank-transactions/" + txId + "/assign"), adminToken, Map.of("orderId", orderId))
                .andExpect(status().isConflict()).andExpect(jsonPath("$.error.code").value("INVALID_BANK_TRANSACTION"));
    }

    @Test
    void transferWithoutOrderCode_isUnmatched_andCanBeIgnoredWithAReason() throws Exception {
        webhook(notice("FT-NONE-1", "ung ho quy", "500000"), SECRET).andExpect(jsonPath("$.data.status").value("UNMATCHED"));
        webhook(notice("FT-NONE-2", "DH99999999", "500000"), SECRET).andExpect(jsonPath("$.data.status").value("UNMATCHED"));
        Map<String, Object> elsewhere = notice("FT-NONE-3", "DH00000001", "500000");
        elsewhere.put("accountNumber", "999888777");
        webhook(elsewhere, SECRET).andExpect(jsonPath("$.data.status").value("UNMATCHED"));

        JsonNode unmatched = list("status", "UNMATCHED").get("content");
        assertThat(unmatched).hasSize(3);
        long txId = unmatched.get(0).get("id").asLong();

        send(post("/api/v1/admin/bank-transactions/" + txId + "/ignore"), adminToken, Map.of("note", " "))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.error.details.note").exists());
        send(post("/api/v1/admin/bank-transactions/" + txId + "/ignore"), adminToken, Map.of("note", "Tiền ủng hộ, không liên quan đơn"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.status").value("IGNORED"));
        send(post("/api/v1/admin/bank-transactions/" + txId + "/ignore"), adminToken, Map.of("note", "again"))
                .andExpect(status().isConflict());
        send(post("/api/v1/admin/bank-transactions/999999999/ignore"), adminToken, Map.of("note", "x"))
                .andExpect(status().isNotFound()).andExpect(jsonPath("$.error.code").value("BANK_TRANSACTION_NOT_FOUND"));
    }

    @Test
    void cancelledOrAlreadyPaidOrder_isNeverConfirmedTwice() throws Exception {
        long cancelled = placeOrder("BANK_TRANSFER");
        patchStatus(cancelled, "CANCELLED").andExpect(status().isOk());
        webhook(notice("FT-CANCEL-1", code(cancelled), PRICE), SECRET)
                .andExpect(jsonPath("$.data.status").value("REVIEW"));

        long paid = placeOrder("BANK_TRANSFER");
        webhook(notice("FT-PAID-1", code(paid), PRICE), SECRET).andExpect(jsonPath("$.data.status").value("MATCHED"));
        webhook(notice("FT-PAID-2", code(paid), PRICE), SECRET).andExpect(jsonPath("$.data.status").value("REVIEW"));

        long cod = placeOrder("COD");
        webhook(notice("FT-COD-1", code(cod), PRICE), SECRET).andExpect(jsonPath("$.data.status").value("REVIEW"));

        JsonNode review = list("status", "REVIEW").get("content");
        assertThat(review).hasSize(3);
        assertThat(review.toString()).contains("hoàn tiền cho khách").contains("đã được thanh toán trước đó")
                .contains("không thanh toán bằng chuyển khoản");
    }

    @Test
    void reconciliation_isForAdminOnly() throws Exception {
        send(get("/api/v1/admin/bank-transactions"), staffToken, null).andExpect(status().isForbidden());
        send(get("/api/v1/admin/bank-transactions"), customerToken, null).andExpect(status().isForbidden());
        send(get("/api/v1/admin/bank-transactions"), null, null).andExpect(status().isUnauthorized());
        send(post("/api/v1/admin/bank-transactions/simulate"), staffToken, Map.of("content", "DH00000001", "amount", 1000))
                .andExpect(status().isForbidden());
        send(get("/api/v1/admin/bank-transactions/info"), staffToken, null).andExpect(status().isForbidden());

        JsonNode info = data(send(get("/api/v1/admin/bank-transactions/info"), adminToken, null).andExpect(status().isOk()));
        assertThat(info.get("webhookConfigured").asBoolean()).isTrue();
        assertThat(info.get("simulatorEnabled").asBoolean()).isTrue();
        assertThat(info.get("accountNumberMasked").asString()).doesNotContain("0123456");
    }

    @Test
    void simulator_goesThroughTheSameMatching() throws Exception {
        long orderId = placeOrder("BANK_TRANSFER");

        send(post("/api/v1/admin/bank-transactions/simulate"), adminToken, Map.of("content", "", "amount", 1000))
                .andExpect(status().isBadRequest());
        send(post("/api/v1/admin/bank-transactions/simulate"), adminToken, Map.of("content", code(orderId), "amount", PRICE))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("MATCHED"));

        patchStatus(orderId, "CONFIRMED").andExpect(status().isOk());
        assertThat(list("status", "MATCHED").get("content").get(0).get("provider").asString()).isEqualTo("SIMULATOR");
    }

    @Test
    void demoConfirm_paysMyOwnTransferOrder_once() throws Exception {
        long orderId = placeOrder("BANK_TRANSFER");
        JsonNode transfer = data(send(get("/api/v1/orders/" + orderId), customerToken, null).andExpect(status().isOk()))
                .get("payment").get("bankTransfer");
        assertThat(transfer.get("demoAutoPay").asBoolean()).isTrue();
        String path = "/api/v1/orders/" + orderId + "/payment/demo-confirm";

        // needs a token, and only the order's owner
        mockMvc.perform(post(path)).andExpect(status().isUnauthorized());
        String other = register("bw.other").get("accessToken").asString();
        send(post(path), other, null).andExpect(status().isNotFound());
        assertThat(paymentStatus(orderId)).isEqualTo("PENDING");

        send(post(path), customerToken, null).andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("MATCHED"));
        assertThat(paymentStatus(orderId)).isEqualTo("PAID");

        // sent again: still fine, no extra row for the ADMIN to review
        send(post(path), customerToken, null).andExpect(status().isOk()).andExpect(jsonPath("$.data.duplicate").value(true));
        assertThat(list("status", "REVIEW").get("content").size()).isZero();
        patchStatus(orderId, "CONFIRMED").andExpect(status().isOk());
    }

    private String paymentStatus(long orderId) throws Exception {
        return data(send(get("/api/v1/orders/" + orderId), customerToken, null).andExpect(status().isOk()))
                .get("payment").get("status").asString();
    }

    // ---------- helpers ----------

    private ResultActions webhook(Map<String, Object> body, String key) throws Exception {
        MockHttpServletRequestBuilder builder = post("/api/v1/payments/webhook");
        if (key != null) {
            builder.header(HttpHeaders.AUTHORIZATION, "Apikey " + key);
        }
        return mockMvc.perform(builder.contentType(MediaType.APPLICATION_JSON).content(jsonMapper.writeValueAsString(body)));
    }

    private Map<String, Object> notice(String id, String content, String amount) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("id", id + "-" + (++sequence));
        body.put("gateway", "Vietcombank");
        body.put("transactionDate", "2026-10-16 21:30:00");
        body.put("accountNumber", "0123456789");
        body.put("content", content);
        body.put("transferType", "in");
        body.put("transferAmount", new BigDecimal(amount));
        return body;
    }

    private static String code(long orderId) {
        return "DH%08d".formatted(orderId);
    }

    private JsonNode list(String... params) throws Exception {
        MockHttpServletRequestBuilder builder = get("/api/v1/admin/bank-transactions");
        for (int i = 0; i < params.length; i += 2) {
            builder.param(params[i], params[i + 1]);
        }
        return data(send(builder, adminToken, null).andExpect(status().isOk()));
    }

    private long placeOrder(String method) throws Exception {
        send(post("/api/v1/cart/items"), customerToken, Map.of("variantId", laptop.getId(), "quantity", 1)).andExpect(status().isOk());
        Map<String, Object> body = new HashMap<>();
        body.put("recipientName", "Nguyễn Văn An");
        body.put("recipientPhone", "0901234567");
        body.put("shippingAddress", "12 Nguyễn Trãi, Quận 5, TP. Hồ Chí Minh");
        body.put("paymentMethod", method);
        return data(send(post("/api/v1/orders"), customerToken, body).andExpect(status().isCreated())).get("id").asLong();
    }

    private ResultActions patchStatus(long orderId, String newStatus) throws Exception {
        return send(patch("/api/v1/admin/orders/" + orderId + "/status"), staffToken, Map.of("status", newStatus));
    }

    private JsonNode register(String username) throws Exception {
        JsonNode data = data(send(post("/api/v1/auth/register"), null, Map.of(
                "email", username + "@example.com", "username", username, "password", PASSWORD,
                "fullname", "Người Dùng Webhook")).andExpect(status().isCreated()));
        registeredUserIds.add(data.get("user").get("id").asLong());
        return data;
    }

    /** Registers, adds the role in the DB, then logs in again so the access token carries it. */
    private String registerWithRole(String username, String role) throws Exception {
        Long id = register(username).get("user").get("id").asLong();
        userRoleRepository.save(new UserRole(userRepository.getReferenceById(id), roleRepository.findByName(role).orElseThrow()));
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
