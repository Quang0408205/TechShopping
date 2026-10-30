package com.example.Tech.controller.payment;

import com.example.Tech.entity.product.Category;
import com.example.Tech.entity.product.Product;
import com.example.Tech.entity.product.ProductVariant;
import com.example.Tech.repository.product.CategoryRepository;
import com.example.Tech.repository.product.ProductRepository;
import com.example.Tech.repository.product.ProductVariantRepository;
import com.example.Tech.security.RefreshTokenService;
import com.example.Tech.service.payment.StripeCheckoutSession;
import com.example.Tech.service.payment.StripeGateway;
import com.example.Tech.service.payment.StripeSignature;
import com.example.Tech.support.StoreFixtures;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Card payment through Stripe (mocked), the Stripe webhook, "Huỷ thanh toán" and the awaited order. */
@SpringBootTest(properties = "app.cors.allowed-origins=http://127.0.0.1:5503")
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class CardPaymentApiIntegrationTest {

    private static final String PASSWORD = "Matkhau@123";
    private static final String RETURN_URL = "http://127.0.0.1:5503/frontend/customer/payment.html";
    private static final String WEBHOOK_SECRET = "whsec_test_only_secret";

    @MockitoBean private StripeGateway stripeGateway;

    @Autowired private MockMvc mockMvc;
    @Autowired private JsonMapper jsonMapper;
    @Autowired private RefreshTokenService refreshTokenService;
    @Autowired private CategoryRepository categoryRepository;
    @Autowired private ProductRepository productRepository;
    @Autowired private ProductVariantRepository variantRepository;
    @Autowired private EntityManager entityManager;

    private final List<Long> registeredUserIds = new ArrayList<>();

    private String customer;
    private String other;
    private ProductVariant phone;
    private ProductVariant cover;

    @BeforeEach
    void setUp() throws Exception {
        customer = token(register("card.khach"));
        other = token(register("card.khac"));
        Category category = new Category();
        category.setName("Danh mục thẻ");
        category.setSlug("test-card-category");
        categoryRepository.save(category);
        phone = variant(product("Điện thoại Thẻ", "test-card-phone", category), "Đen", "14990000");
        cover = variant(product("Ốp Thẻ", "test-card-cover", category), "Trong", "200000");
        StoreFixtures.warehouse(entityManager, phone, cover);
        entityManager.flush();
    }

    @AfterEach
    void revokeTokens() {
        registeredUserIds.forEach(refreshTokenService::revokeAll);
    }

    @Test
    void cardOrder_opensOneStripePage_isPaidWhenTheCustomerComesBack() throws Exception {
        send(get("/api/v1/payments/card-options"), customer, null)
                .andExpect(jsonPath("$.data.enabled").value(true))
                .andExpect(jsonPath("$.data.testMode").value(true))
                .andExpect(jsonPath("$.data.maxAmount").value(99999999));
        JsonNode order = placeOrder("CARD");
        long orderId = order.get("id").asLong();
        long total = order.get("total").decimalValue().longValueExact();
        assertThat(order.get("payment").get("status").asString()).isEqualTo("PENDING");
        assertThat(order.get("payment").get("card").get("testMode").asBoolean()).isTrue();
        assertThat(order.get("payment").get("bankTransfer").isNull()).isTrue();

        send(post("/api/v1/orders/" + orderId + "/payment/card-checkout"), customer,
                Map.of("returnUrl", "https://evil.example.com/customer/payment.html"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.details.returnUrl").exists());
        send(post("/api/v1/orders/" + orderId + "/payment/card-checkout"), customer, Map.of("returnUrl", RETURN_URL + "?x=1"))
                .andExpect(status().isBadRequest());
        send(post("/api/v1/orders/" + orderId + "/payment/card-checkout"), other, Map.of("returnUrl", RETURN_URL))
                .andExpect(status().isNotFound());

        when(stripeGateway.createSession(any())).thenReturn(session("cs_test_1", "open", "unpaid", total, null, orderId));
        send(post("/api/v1/orders/" + orderId + "/payment/card-checkout"), customer, Map.of("returnUrl", RETURN_URL))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.checkoutUrl").value("https://checkout.stripe.test/cs_test_1"))
                .andExpect(jsonPath("$.data.paid").value(false));
        ArgumentCaptor<StripeGateway.NewSession> created = ArgumentCaptor.forClass(StripeGateway.NewSession.class);
        verify(stripeGateway).createSession(created.capture());
        assertThat(created.getValue().amountVnd()).isEqualTo(total);
        assertThat(created.getValue().orderCode()).isEqualTo(order.get("code").asString());
        assertThat(created.getValue().successUrl()).isEqualTo(RETURN_URL + "?id=" + orderId + "&card=paid");
        assertThat(created.getValue().cancelUrl()).isEqualTo(RETURN_URL + "?id=" + orderId + "&card=back");
        assertThat(created.getValue().customerEmail()).isEqualTo("card.khach@example.com");

        // still open: the same page again, no second session
        when(stripeGateway.getSession("cs_test_1")).thenReturn(session("cs_test_1", "open", "unpaid", total, null, orderId));
        send(post("/api/v1/orders/" + orderId + "/payment/card-checkout"), customer, Map.of("returnUrl", RETURN_URL))
                .andExpect(jsonPath("$.data.checkoutUrl").value("https://checkout.stripe.test/cs_test_1"));
        verify(stripeGateway, times(1)).createSession(any());
        send(get("/api/v1/orders/awaiting-payment"), customer, null)
                .andExpect(jsonPath("$.data.id").value(orderId))
                .andExpect(jsonPath("$.data.paymentMethod").value("CARD"));

        when(stripeGateway.getSession("cs_test_1")).thenReturn(session("cs_test_1", "complete", "paid", total, "pi_test_1", orderId));
        send(post("/api/v1/orders/" + orderId + "/payment/card-refresh"), customer, null)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.payment.status").value("PAID"))
                .andExpect(jsonPath("$.data.payment.transactionId").value("pi_test_1"))
                .andExpect(jsonPath("$.data.payment.card").doesNotExist());
        send(get("/api/v1/orders/awaiting-payment"), customer, null).andExpect(jsonPath("$.data").doesNotExist());
        send(post("/api/v1/orders/" + orderId + "/cancel-payment"), customer, null)
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error.code").value("INVALID_PAYMENT_STATUS"));
        send(post("/api/v1/orders/" + orderId + "/payment/card-checkout"), customer, Map.of("returnUrl", RETURN_URL))
                .andExpect(status().isConflict());
    }

    @Test
    void signedWebhook_marksTheCardOrderPaid_onlyForTheRightAmount() throws Exception {
        JsonNode order = placeOrder("CARD");
        long orderId = order.get("id").asLong();
        long total = order.get("total").decimalValue().longValueExact();

        String wrongAmount = event("checkout.session.completed", "cs_test_9", total - 1, orderId);
        webhook(wrongAmount, sign(wrongAmount)).andExpect(status().isOk());
        assertThat(paymentStatus(orderId)).isEqualTo("PENDING");

        String paid = event("checkout.session.completed", "cs_test_9", total, orderId);
        webhook(paid, "t=1,v1=00").andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("STRIPE_SIGNATURE_INVALID"));
        webhook(paid, null).andExpect(status().isBadRequest());
        assertThat(paymentStatus(orderId)).isEqualTo("PENDING");

        webhook(paid, sign(paid)).andExpect(status().isOk());
        assertThat(paymentStatus(orderId)).isEqualTo("PAID");
        webhook(paid, sign(paid)).andExpect(status().isOk());
        assertThat(paymentStatus(orderId)).isEqualTo("PAID");

        String other = event("checkout.session.expired", "cs_test_8", total, orderId);
        webhook(other, sign(other)).andExpect(status().isOk());
    }

    @Test
    void cancelPayment_cancelsTheTransferOrder_andPutsTheSameItemsBackInTheCart() throws Exception {
        JsonNode order = placeOrder("BANK_TRANSFER");
        long orderId = order.get("id").asLong();
        send(get("/api/v1/cart"), customer, null).andExpect(jsonPath("$.data.items.length()").value(0));
        send(get("/api/v1/orders/awaiting-payment"), customer, null).andExpect(jsonPath("$.data.id").value(orderId));
        // something added meanwhile stays and is merged
        send(post("/api/v1/cart/items"), customer, Map.of("variantId", cover.getId(), "quantity", 1)).andExpect(status().isOk());

        send(post("/api/v1/orders/" + orderId + "/cancel-payment"), other, null).andExpect(status().isNotFound());
        send(post("/api/v1/orders/" + orderId + "/cancel-payment"), customer, null)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("CANCELLED"))
                .andExpect(jsonPath("$.data.payment.status").value("CANCELLED"));

        JsonNode cart = data(send(get("/api/v1/cart"), customer, null));
        Map<Long, Integer> quantities = new HashMap<>();
        cart.get("items").forEach(item -> quantities.put(item.get("variantId").asLong(), item.get("quantity").asInt()));
        assertThat(quantities).containsEntry(phone.getId(), 1).containsEntry(cover.getId(), 3).hasSize(2);
        send(get("/api/v1/orders/awaiting-payment"), customer, null).andExpect(jsonPath("$.data").doesNotExist());
        send(post("/api/v1/orders/" + orderId + "/cancel-payment"), customer, null).andExpect(status().isConflict());
        verify(stripeGateway, never()).expireSession(any());
    }

    @Test
    void cancelPayment_isOnlyForUnpaidTransferOrCardOrders() throws Exception {
        long codOrder = placeOrder("COD").get("id").asLong();

        send(post("/api/v1/orders/" + codOrder + "/cancel-payment"), customer, null)
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error.code").value("INVALID_PAYMENT_STATUS"));
        send(get("/api/v1/orders/awaiting-payment"), customer, null).andExpect(jsonPath("$.data").doesNotExist());
        send(get("/api/v1/orders/awaiting-payment"), null, null).andExpect(status().isUnauthorized());
    }

    @Test
    void cardAboveStripesLimit_isRefusedAtCheckout() throws Exception {
        ProductVariant expensive = variant(product("Máy chủ Thẻ", "test-card-server", phone.getProduct().getCategory()),
                "Bạc", "60000000");
        StoreFixtures.warehouse(entityManager, expensive);
        entityManager.flush();
        send(post("/api/v1/cart/items"), customer, Map.of("variantId", expensive.getId(), "quantity", 2)).andExpect(status().isOk());

        send(post("/api/v1/orders"), customer, orderBody("CARD"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("CARD_AMOUNT_TOO_LARGE"));
    }

    private JsonNode placeOrder(String method) throws Exception {
        send(post("/api/v1/cart/items"), customer, Map.of("variantId", phone.getId(), "quantity", 1)).andExpect(status().isOk());
        send(post("/api/v1/cart/items"), customer, Map.of("variantId", cover.getId(), "quantity", 2)).andExpect(status().isOk());
        return data(send(post("/api/v1/orders"), customer, orderBody(method)).andExpect(status().isCreated()));
    }

    private static Map<String, Object> orderBody(String method) {
        Map<String, Object> body = new HashMap<>();
        body.put("recipientName", "Khách Thẻ");
        body.put("recipientPhone", "0901234567");
        body.put("shippingAddress", "1 Lê Lợi, Phường Bến Thành, Thành phố Hồ Chí Minh");
        body.put("paymentMethod", method);
        return body;
    }

    private static StripeCheckoutSession session(String id, String status, String paymentStatus, long amount,
                                                 String paymentIntent, long orderId) {
        return new StripeCheckoutSession(id, "https://checkout.stripe.test/" + id, status, paymentStatus, amount, "vnd",
                paymentIntent, orderId);
    }

    private static String event(String type, String sessionId, long amount, long orderId) {
        return ("{\"id\":\"evt_%s\",\"type\":\"%s\",\"data\":{\"object\":{\"id\":\"%s\",\"object\":\"checkout.session\","
                + "\"status\":\"complete\",\"payment_status\":\"paid\",\"amount_total\":%d,\"currency\":\"vnd\","
                + "\"payment_intent\":\"pi_%s\",\"client_reference_id\":\"%d\"}}}")
                .formatted(sessionId, type, sessionId, amount, sessionId, orderId);
    }

    private static String sign(String payload) {
        long now = Instant.now().getEpochSecond();
        return "t=" + now + ",v1=" + StripeSignature.sign(now + "." + payload, WEBHOOK_SECRET);
    }

    private ResultActions webhook(String payload, String signature) throws Exception {
        MockHttpServletRequestBuilder request = post("/api/v1/payments/stripe/webhook")
                .contentType(MediaType.APPLICATION_JSON).content(payload);
        if (signature != null) {
            request.header("Stripe-Signature", signature);
        }
        return mockMvc.perform(request);
    }

    private String paymentStatus(long orderId) {
        entityManager.flush();
        entityManager.clear();
        return (String) entityManager.createNativeQuery(
                        "select status from payments where order_id = :id and installment_payment_id is null")
                .setParameter("id", orderId).getSingleResult();
    }

    private Product product(String name, String slug, Category category) {
        Product product = new Product();
        product.setName(name);
        product.setSlug(slug);
        product.setCategory(category);
        product.setBasePrice(new BigDecimal("1000"));
        return productRepository.save(product);
    }

    private ProductVariant variant(Product product, String name, String price) {
        ProductVariant variant = new ProductVariant();
        variant.setProduct(product);
        variant.setVariantName(name);
        variant.setPrice(new BigDecimal(price));
        return variantRepository.save(variant);
    }

    private JsonNode register(String username) throws Exception {
        JsonNode data = data(send(post("/api/v1/auth/register"), null, Map.of(
                "email", username + "@example.com", "username", username, "password", PASSWORD,
                "fullname", "Người Thử Thẻ"))
                .andExpect(status().isCreated()));
        registeredUserIds.add(data.get("user").get("id").asLong());
        return data;
    }

    private static String token(JsonNode auth) {
        return auth.get("accessToken").asString();
    }

    private ResultActions send(MockHttpServletRequestBuilder builder, String accessToken, Object body) throws Exception {
        if (accessToken != null) {
            builder.header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken);
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
