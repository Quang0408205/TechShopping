package com.example.Tech.controller.order;

import com.example.Tech.entity.product.Category;
import com.example.Tech.entity.store.Store;
import com.example.Tech.entity.product.Product;
import com.example.Tech.entity.product.ProductImage;
import com.example.Tech.entity.product.ProductVariant;
import com.example.Tech.repository.order.OrderRepository;
import com.example.Tech.repository.store.StoreRepository;
import com.example.Tech.support.StoreFixtures;
import com.example.Tech.repository.payment.InstallmentOrderRepository;
import com.example.Tech.repository.product.CategoryRepository;
import com.example.Tech.repository.product.ProductImageRepository;
import com.example.Tech.repository.product.ProductRepository;
import com.example.Tech.repository.product.ProductVariantRepository;
import com.example.Tech.security.RefreshTokenService;
import com.example.Tech.service.product.ProductService;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.data.domain.PageRequest;
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
import static org.hamcrest.Matchers.matchesPattern;
import static org.hamcrest.Matchers.notNullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * /api/v1/orders (and the cart shipping fee) over HTTP against the test database (rolled back) and the real
 * Redis (refresh tokens of the registered users are revoked afterwards). Builds its own catalogue.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class OrderApiIntegrationTest {

    private static final String PASSWORD = "Matkhau@123";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JsonMapper jsonMapper;

    @Autowired
    private RefreshTokenService refreshTokenService;

    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private ProductVariantRepository variantRepository;

    @Autowired
    private ProductImageRepository imageRepository;

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private InstallmentOrderRepository installmentOrderRepository;

    @Autowired
    private StoreRepository storeRepository;

    @Autowired
    private ProductService productService;

    @Autowired
    private EntityManager entityManager;

    private final List<Long> registeredUserIds = new ArrayList<>();

    private Long userId;
    private String token;
    private Product phone;
    private ProductVariant black;
    private ProductVariant white;
    private ProductVariant cover;
    private Store warehouse;

    @BeforeEach
    void setUp() throws Exception {
        JsonNode auth = register("order.test");
        userId = auth.get("user").get("id").asLong();
        token = auth.get("accessToken").asString();

        Category category = new Category();
        category.setName("Điện thoại");
        category.setSlug("test-order-api-dien-thoai");
        category = categoryRepository.save(category);

        phone = productRepository.save(product("Điện thoại Đơn Hàng X", "test-order-api-x", category, "15990000"));
        black = variantRepository.save(variant(phone, "Đen 128GB", "15990000", "14990000"));
        white = variantRepository.save(variant(phone, "Trắng 256GB", "18990000", null));
        Product accessory = productRepository.save(product("Ốp lưng Đơn Hàng", "test-order-api-op", category, "200000"));
        cover = variantRepository.save(variant(accessory, "Trong suốt", "200000", null));

        ProductImage image = new ProductImage();
        image.setProduct(phone);
        image.setImageUrl("https://cdn.example/order-x.jpg");
        image.setPrimary(true);
        imageRepository.save(image);
        warehouse = StoreFixtures.warehouse(entityManager, black, white, cover);
        entityManager.flush();
    }

    @AfterEach
    void revokeTokens() {
        registeredUserIds.forEach(refreshTokenService::revokeAll);
    }

    @Test
    void orders_requireToken() throws Exception {
        send(get("/api/v1/orders"), null, null)
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error.code").value("UNAUTHORIZED"));
        send(post("/api/v1/orders"), null, orderBody())
                .andExpect(status().isUnauthorized());
    }

    @Test
    void placeOrder_fromTheServerCart_thenTheCartIsEmpty_andASecondSubmitIsRefused() throws Exception {
        addToCart(black, 2);
        addToCart(white, 1);
        send(get("/api/v1/cart"), token, null)
                .andExpect(jsonPath("$.data.subtotal").value(2 * 14990000L + 18990000L))
                .andExpect(jsonPath("$.data.shippingFee").value(0))
                .andExpect(jsonPath("$.data.total").value(2 * 14990000L + 18990000L));

        JsonNode order = data(send(post("/api/v1/orders"), token, orderBody())
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.code", matchesPattern("DH\\d{8}")))
                .andExpect(jsonPath("$.data.status").value("PENDING"))
                .andExpect(jsonPath("$.data.paymentMethod").value("COD"))
                .andExpect(jsonPath("$.data.recipientName").value("Nguyễn Văn An"))
                .andExpect(jsonPath("$.data.note").value("Gọi trước khi giao"))
                .andExpect(jsonPath("$.data.items.length()").value(2))
                .andExpect(jsonPath("$.data.items[0].variantId").value(black.getId()))
                .andExpect(jsonPath("$.data.items[0].productName").value("Điện thoại Đơn Hàng X"))
                .andExpect(jsonPath("$.data.items[0].imageUrl").value("https://cdn.example/order-x.jpg"))
                .andExpect(jsonPath("$.data.items[0].unitPrice").value(14990000))
                .andExpect(jsonPath("$.data.items[0].originalPrice").value(15990000))
                .andExpect(jsonPath("$.data.items[0].discountAmount").value(2000000))
                .andExpect(jsonPath("$.data.items[0].subtotal").value(29980000))
                .andExpect(jsonPath("$.data.items[1].originalPrice").doesNotExist())
                .andExpect(jsonPath("$.data.totalQuantity").value(3))
                .andExpect(jsonPath("$.data.subtotal").value(48970000))
                .andExpect(jsonPath("$.data.shippingFee").value(0))
                .andExpect(jsonPath("$.data.total").value(48970000))
                .andExpect(jsonPath("$.data.orderDate", notNullValue()))
                .andExpect(jsonPath("$.data.cancellable").value(true)));
        assertThat(order.get("code").asString()).isEqualTo("DH%08d".formatted(order.get("id").asLong()));

        send(get("/api/v1/cart"), token, null)
                .andExpect(jsonPath("$.data.items.length()").value(0))
                .andExpect(jsonPath("$.data.total").value(0));
        send(post("/api/v1/orders"), token, orderBody())
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error.code").value("CART_EMPTY"));
        assertThat(orderRepository.findAllByUserId(userId, PageRequest.of(0, 10)).getTotalElements()).isEqualTo(1);
    }

    @Test
    void placeOrder_below10Million_chargesTheShippingFee_andKeepsTheCheckoutPrice() throws Exception {
        addToCart(cover, 2);
        send(get("/api/v1/cart"), token, null)
                .andExpect(jsonPath("$.data.shippingFee").value(30000))
                .andExpect(jsonPath("$.data.total").value(430000));

        long orderId = data(send(post("/api/v1/orders"), token, orderBody())
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.shippingFee").value(30000))
                .andExpect(jsonPath("$.data.total").value(430000))).get("id").asLong();

        // a later price change does not touch the order
        cover.setPrice(new BigDecimal("999000"));
        variantRepository.saveAndFlush(cover);
        send(get("/api/v1/orders/" + orderId), token, null)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.items[0].unitPrice").value(200000))
                .andExpect(jsonPath("$.data.total").value(430000));
    }

    @Test
    void placeOrder_withAnUnavailableProduct_isRefusedAndTheCartIsKept() throws Exception {
        addToCart(black, 1);
        productService.delete(phone.getId());
        entityManager.flush();

        send(post("/api/v1/orders"), token, orderBody())
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error.code").value("PRODUCT_NOT_AVAILABLE"));
        send(get("/api/v1/cart"), token, null)
                .andExpect(jsonPath("$.data.items.length()").value(1));
    }

    @Test
    void placeOrder_invalidInput_isRejectedBeforeTouchingTheCart() throws Exception {
        addToCart(cover, 1);

        send(post("/api/v1/orders"), token, Map.of())
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.error.details.recipientName").exists())
                .andExpect(jsonPath("$.error.details.recipientPhone").exists())
                .andExpect(jsonPath("$.error.details.paymentMethod").exists());
        // the address is only required for home delivery, so it is checked after the field rules
        Map<String, Object> noAddress = orderBody();
        noAddress.remove("shippingAddress");
        send(post("/api/v1/orders"), token, noAddress)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.details.shippingAddress").exists());
        Map<String, Object> badPhone = orderBody();
        badPhone.put("recipientPhone", "gọi tôi");
        send(post("/api/v1/orders"), token, badPhone)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.details.recipientPhone").exists());
        Map<String, Object> badMethod = orderBody();
        badMethod.put("paymentMethod", "BITCOIN");
        send(post("/api/v1/orders"), token, badMethod)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("MALFORMED_REQUEST"));

        send(get("/api/v1/cart"), token, null)
                .andExpect(jsonPath("$.data.items.length()").value(1));
    }

    @Test
    void homeDelivery_goesToTheStoreOfTheAddressDistrict() throws Exception {
        Store q1 = storeRepository.save(store("ZZ DH Quận 1", "Quận 1", true));
        Store q5 = storeRepository.save(store("ZZ DH Quận 5", "Quận 5", true));
        entityManager.flush();
        StoreFixtures.stock(entityManager, q1, cover, 5);
        StoreFixtures.stock(entityManager, q5, cover, 5);
        addToCart(cover, 1);

        JsonNode order = data(send(post("/api/v1/orders"), token, orderBody())
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.deliveryType").value("HOME_DELIVERY"))
                .andExpect(jsonPath("$.data.shippingFee").value(30000)));

        assertThat(order.get("storeId").asInt()).isEqualTo(q5.getId());
        assertThat(order.get("storeName").asString()).isEqualTo("ZZ DH Quận 5");
        assertThat(orderRepository.findById(order.get("id").asLong()).orElseThrow().getStore().getId())
                .isEqualTo(q5.getId())
                .isNotEqualTo(q1.getId());
    }

    @Test
    void pickup_atTheChosenStore_isFreeOfShipping_andNeedsNoAddress() throws Exception {
        Store q1 = storeRepository.save(store("ZZ DH Quận 1", "Quận 1", true));
        entityManager.flush();
        StoreFixtures.stock(entityManager, q1, cover, 2);
        addToCart(cover, 2);
        Map<String, Object> body = orderBody();
        body.remove("shippingAddress");
        body.put("deliveryType", "PICKUP");
        body.put("pickupStoreId", q1.getId());

        long orderId = data(send(post("/api/v1/orders"), token, body)
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.deliveryType").value("PICKUP"))
                .andExpect(jsonPath("$.data.storeId").value(q1.getId()))
                .andExpect(jsonPath("$.data.shippingAddress")
                        .value("Nhận tại cửa hàng: ZZ DH Quận 1, 1 Đường Test, Quận 1, Hồ Chí Minh"))
                .andExpect(jsonPath("$.data.shippingFee").value(0))
                .andExpect(jsonPath("$.data.total").value(400000))
                .andExpect(jsonPath("$.data.payment.amount").value(400000))).get("id").asLong();

        send(get("/api/v1/orders/" + orderId), token, null)
                .andExpect(jsonPath("$.data.storeName").value("ZZ DH Quận 1"));
    }

    @Test
    void pickup_withoutAStore_atAClosedStore_orHomeDeliveryWithAStore_isRefused_andTheCartIsKept() throws Exception {
        Store closed = storeRepository.save(store("ZZ DH Đã đóng", "Quận 3", false));
        entityManager.flush();
        addToCart(cover, 1);

        Map<String, Object> noStore = orderBody();
        noStore.put("deliveryType", "PICKUP");
        send(post("/api/v1/orders"), token, noStore)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.details.pickupStoreId").exists());
        Map<String, Object> closedStore = orderBody();
        closedStore.put("deliveryType", "PICKUP");
        closedStore.put("pickupStoreId", closed.getId());
        send(post("/api/v1/orders"), token, closedStore)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.details.pickupStoreId").exists());
        Map<String, Object> homeWithStore = orderBody();
        homeWithStore.put("pickupStoreId", closed.getId());
        send(post("/api/v1/orders"), token, homeWithStore)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.details.pickupStoreId").exists());

        send(get("/api/v1/cart"), token, null)
                .andExpect(jsonPath("$.data.items.length()").value(1));
    }

    @Test
    void homeDelivery_onlyStoresHoldingTheWholeCart_count_elseOutOfStock_andTheCartIsKept() throws Exception {
        Store q5 = storeRepository.save(store("ZZ DH Quận 5", "Quận 5", true));
        entityManager.flush();
        StoreFixtures.stock(entityManager, q5, cover, 5);
        addToCart(cover, 1);
        addToCart(black, 1);

        // Quận 5 is nearer but lacks the phone → the warehouse holding both handles the order
        JsonNode order = data(send(post("/api/v1/orders"), token, orderBody()).andExpect(status().isCreated()));
        assertThat(order.get("storeId").asInt()).isEqualTo(warehouse.getId());

        // nobody holds both any more: the phone only at Quận 5, the cover only at the (closed) warehouse
        warehouse.setActive(false);
        storeRepository.saveAndFlush(entityManager.merge(warehouse));
        StoreFixtures.stock(entityManager, q5, black, 1);
        Store other = storeRepository.save(store("ZZ DH Quận 7", "Quận 7", true));
        entityManager.flush();
        StoreFixtures.stock(entityManager, other, white, 3);
        entityManager.createNativeQuery("update inventory set quantity = 0 where store_id = :s and variant_id = :v")
                .setParameter("s", q5.getId()).setParameter("v", cover.getId()).executeUpdate();
        entityManager.clear();
        StoreFixtures.stock(entityManager, other, cover, 1);
        addToCart(cover, 1);
        addToCart(black, 1);
        send(post("/api/v1/orders"), token, orderBody())
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error.code").value("OUT_OF_STOCK"))
                .andExpect(jsonPath("$.error.message").value(org.hamcrest.Matchers.startsWith("Không chi nhánh nào có đủ cả đơn")));
        send(get("/api/v1/cart"), token, null)
                .andExpect(jsonPath("$.data.items.length()").value(2));

        // more than all open stores hold → the cart refuses the line before checkout
        send(post("/api/v1/cart/items"), token, Map.of("variantId", white.getId(), "quantity", 4))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error.code").value("OUT_OF_STOCK"));
    }

    @Test
    void pickup_atAStoreLackingItems_isOutOfStock_andStoreAvailabilityTellsWhichStoresHoldTheCart() throws Exception {
        Store q1 = storeRepository.save(store("ZZ DH Quận 1", "Quận 1", true));
        entityManager.flush();
        StoreFixtures.stock(entityManager, q1, cover, 1);
        addToCart(cover, 2);

        JsonNode stores = data(send(get("/api/v1/cart/store-availability"), token, null).andExpect(status().isOk()));
        JsonNode atQ1 = null;
        JsonNode atWarehouse = null;
        for (JsonNode store : stores) {
            if (store.get("storeId").asInt() == q1.getId()) {
                atQ1 = store;
            } else if (store.get("storeId").asInt() == warehouse.getId()) {
                atWarehouse = store;
            }
        }
        assertThat(atWarehouse.get("available").asBoolean()).isTrue();
        assertThat(atQ1.get("available").asBoolean()).isFalse();
        assertThat(atQ1.get("missing").get(0).asString()).isEqualTo("Ốp lưng Đơn Hàng - Trong suốt (cần 2, còn 1)");

        Map<String, Object> body = orderBody();
        body.remove("shippingAddress");
        body.put("deliveryType", "PICKUP");
        body.put("pickupStoreId", q1.getId());
        send(post("/api/v1/orders"), token, body)
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error.code").value("OUT_OF_STOCK"))
                .andExpect(jsonPath("$.error.message").value(
                        "Chi nhánh ZZ DH Quận 1 không đủ hàng: Ốp lưng Đơn Hàng - Trong suốt (cần 2, còn 1). "
                                + "Hãy chọn chi nhánh khác hoặc giao tận nhà"));
        body.put("pickupStoreId", warehouse.getId());
        send(post("/api/v1/orders"), token, body).andExpect(status().isCreated());
    }

    @Test
    void productAndVariantResponses_showTheUnitsOfOpenStores() throws Exception {
        send(get("/api/v1/products/" + phone.getId()), null, null)
                .andExpect(jsonPath("$.data.availableQuantity").value(100));
        send(get("/api/v1/products/" + phone.getId() + "/variants"), null, null)
                .andExpect(jsonPath("$.data[0].availableQuantity").value(50));
        warehouse.setActive(false);
        storeRepository.saveAndFlush(entityManager.merge(warehouse));
        send(get("/api/v1/products/" + phone.getId()), null, null)
                .andExpect(jsonPath("$.data.availableQuantity").value(0));
    }

    @Test
    void myOrders_newestFirst_otherUsersCannotSeeOrCancelThem() throws Exception {
        addToCart(cover, 1);
        long first = data(send(post("/api/v1/orders"), token, orderBody())).get("id").asLong();
        addToCart(black, 1);
        long second = data(send(post("/api/v1/orders"), token, orderBody())).get("id").asLong();

        send(get("/api/v1/orders"), token, null)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalElements").value(2))
                .andExpect(jsonPath("$.data.content[0].id").value(second))
                .andExpect(jsonPath("$.data.content[1].id").value(first))
                .andExpect(jsonPath("$.data.content[1].items[0].variantId").value(cover.getId()));

        String otherToken = register("order.other").get("accessToken").asString();
        send(get("/api/v1/orders"), otherToken, null)
                .andExpect(jsonPath("$.data.totalElements").value(0));
        send(get("/api/v1/orders/" + first), otherToken, null)
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error.code").value("ORDER_NOT_FOUND"));
        send(post("/api/v1/orders/" + first + "/cancel"), otherToken, null)
                .andExpect(status().isNotFound());
        send(get("/api/v1/orders/999999999"), token, null)
                .andExpect(status().isNotFound());
    }

    @Test
    void cancel_pendingOrder_onlyOnce() throws Exception {
        addToCart(cover, 1);
        long orderId = data(send(post("/api/v1/orders"), token, orderBody())).get("id").asLong();

        send(post("/api/v1/orders/" + orderId + "/cancel"), token, null)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("CANCELLED"))
                .andExpect(jsonPath("$.data.cancelledAt", notNullValue()))
                .andExpect(jsonPath("$.data.cancellable").value(false));
        send(post("/api/v1/orders/" + orderId + "/cancel"), token, null)
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error.code").value("INVALID_ORDER_STATUS"));
    }

    @Test
    void codOrder_hasAPendingPayment_cancelledWithTheOrder() throws Exception {
        addToCart(cover, 1);
        long orderId = data(send(post("/api/v1/orders"), token, orderBody())
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.payment.status").value("PENDING"))
                .andExpect(jsonPath("$.data.payment.amount").value(230000))
                .andExpect(jsonPath("$.data.payment.bankTransfer").doesNotExist())
                .andExpect(jsonPath("$.data.installment").doesNotExist())).get("id").asLong();

        send(post("/api/v1/orders/" + orderId + "/cancel"), token, null)
                .andExpect(jsonPath("$.data.payment.status").value("CANCELLED"));
        send(get("/api/v1/orders"), token, null)
                .andExpect(jsonPath("$.data.content[0].payment.status").value("CANCELLED"));
    }

    @Test
    void bankTransferOrder_showsTheTransferInstructions() throws Exception {
        addToCart(black, 1);
        JsonNode order = data(send(post("/api/v1/orders"), token, orderBody("BANK_TRANSFER"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.payment.status").value("PENDING"))
                .andExpect(jsonPath("$.data.payment.bankTransfer.bankName").value("Vietcombank"))
                .andExpect(jsonPath("$.data.payment.bankTransfer.accountNumber").value("0123456789"))
                .andExpect(jsonPath("$.data.payment.bankTransfer.accountName").value("CONG TY POY"))
                .andExpect(jsonPath("$.data.payment.bankTransfer.amount").value(14990000))
                .andExpect(jsonPath("$.data.payment.bankTransfer.payBefore", notNullValue())));
        String code = order.get("code").asString();
        JsonNode transfer = order.get("payment").get("bankTransfer");

        assertThat(transfer.get("transferContent").asString()).isEqualTo(code);
        assertThat(transfer.get("qrImageUrl").asString())
                .startsWith("https://img.vietqr.io/image/970436-0123456789-compact2.png?amount=14990000&addInfo=" + code);
    }

    @Test
    void installmentOrder_sendsTheApplicationToThePartner_citizenIdMaskedAndFeeHiddenFromTheCustomer() throws Exception {
        addToCart(black, 1);
        long orderId = data(send(post("/api/v1/orders"), token, installmentBody(6, " 0123456789 ", "TCB"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.paymentMethod").value("INSTALLMENT"))
                .andExpect(jsonPath("$.data.payment.status").value("PENDING"))
                .andExpect(jsonPath("$.data.payment.bankTransfer").doesNotExist())
                .andExpect(jsonPath("$.data.installment.status").value("PENDING_APPROVAL"))
                .andExpect(jsonPath("$.data.installment.partner").value("CREDIT_CARD"))
                .andExpect(jsonPath("$.data.installment.partnerName").value("Thẻ tín dụng"))
                .andExpect(jsonPath("$.data.installment.partnerRef").doesNotExist())
                .andExpect(jsonPath("$.data.installment.feeAmount").doesNotExist())
                .andExpect(jsonPath("$.data.installment.numMonths").value(6))
                .andExpect(jsonPath("$.data.installment.monthlyPayment").value(2498333))
                .andExpect(jsonPath("$.data.installment.lastPayment").value(2498335))
                .andExpect(jsonPath("$.data.installment.totalAmount").value(14990000))
                .andExpect(jsonPath("$.data.installment.interestRate").value(0))
                .andExpect(jsonPath("$.data.installment.citizenId").value("******6789"))
                .andExpect(jsonPath("$.data.installment.cardBank").value("TCB"))
                .andExpect(jsonPath("$.data.installment.cardBankName").value("Techcombank"))
                .andExpect(jsonPath("$.data.installment.remainingAmount").value(14990000))
                .andExpect(jsonPath("$.data.installment.periods.length()").value(0))).get("id").asLong();
        assertThat(installmentOrderRepository.findByOrderId(orderId).orElseThrow().getCitizenId()).isEqualTo("0123456789");

        send(get("/api/v1/orders/" + orderId), token, null)
                .andExpect(jsonPath("$.data.installment.citizenId").value("******6789"));
        send(post("/api/v1/orders/" + orderId + "/cancel"), token, null)
                .andExpect(jsonPath("$.data.installment.status").value("CANCELLED"))
                .andExpect(jsonPath("$.data.installment.remainingAmount").value(0));
    }

    @Test
    void installmentOrder_invalidApplications_areRefused_andTheCartIsKept() throws Exception {
        addToCart(black, 1);

        send(post("/api/v1/orders"), token, orderBody("INSTALLMENT"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.error.details.installment").value("Vui lòng nhập thông tin trả góp"));
        send(post("/api/v1/orders"), token, installmentBody(5, "0123456789", "VCB"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.details['installment.months']").exists());
        send(post("/api/v1/orders"), token, installmentBody(6, "01234567", "VCB"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.details['installment.citizenId']").value("Số CCCD phải gồm đúng 10 chữ số"));
        send(post("/api/v1/orders"), token, installmentBody(6, "012345678901", "VCB"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.details['installment.citizenId']").exists());
        send(post("/api/v1/orders"), token, installmentBody(6, "0123456789", "XYZ"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("MALFORMED_REQUEST"));
        Map<String, Object> codWithInstallment = installmentBody(6, "0123456789", "VCB");
        codWithInstallment.put("paymentMethod", "COD");
        send(post("/api/v1/orders"), token, codWithInstallment)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.details.installment").exists());

        send(get("/api/v1/cart"), token, null)
                .andExpect(jsonPath("$.data.items.length()").value(1));
        assertThat(orderRepository.findAllByUserId(userId, PageRequest.of(0, 10)).getTotalElements()).isZero();
    }

    @Test
    void installmentOrder_belowThreeMillion_isNotEligible() throws Exception {
        addToCart(cover, 2);

        send(post("/api/v1/orders"), token, installmentBody(3, "0123456789", "VCB"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("INSTALLMENT_NOT_ELIGIBLE"))
                .andExpect(jsonPath("$.error.message").value("Thẻ tín dụng chỉ trả góp đơn hàng từ 3.000.000đ"));
        send(get("/api/v1/cart"), token, null)
                .andExpect(jsonPath("$.data.items.length()").value(1));
    }

    @Test
    void installmentOrder_withAPartner_followsItsTermsCardBankAndOrderRange() throws Exception {
        addToCart(black, 1);

        Map<String, Object> noPartner = installmentBody(6, "0123456789", "VCB");
        ((Map<String, Object>) noPartner.get("installment")).remove("cardBank");
        send(post("/api/v1/orders"), token, noPartner)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.details['installment.partner']").exists());
        send(post("/api/v1/orders"), token, partnerBody("NO_SUCH", 6, null))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.details['installment.partner']").exists());
        send(post("/api/v1/orders"), token, partnerBody("CREDIT_CARD", 6, null))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.details['installment.cardBank']").exists());
        send(post("/api/v1/orders"), token, partnerBody("FINANCE_A", 6, "VCB"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.details['installment.cardBank']").exists());
        send(post("/api/v1/orders"), token, partnerBody("FINANCE_A", 3, null))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.details['installment.months']").value("Công ty tài chính A không hỗ trợ kỳ hạn 3 tháng"));
        // 14.990.000đ is inside the 15.000.000đ limit of company B; two phones are not
        send(post("/api/v1/orders"), token, partnerBody("FINANCE_B", 6, null))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.installment.partner").value("FINANCE_B"))
                .andExpect(jsonPath("$.data.installment.cardBank").doesNotExist())
                .andExpect(jsonPath("$.data.installment.attemptCount").value(1));

        addToCart(black, 2);
        send(post("/api/v1/orders"), token, partnerBody("FINANCE_B", 6, null))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("INSTALLMENT_NOT_ELIGIBLE"))
                .andExpect(jsonPath("$.error.message").value(org.hamcrest.Matchers.containsString("Công ty tài chính B")));
        send(post("/api/v1/orders"), token, partnerBody("FINANCE_A", 12, null))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.installment.partner").value("FINANCE_A"));
    }

    @Test
    void installmentOptions_listTheTermsAndBanks() throws Exception {
        send(get("/api/v1/payments/installment-options"), null, null)
                .andExpect(status().isUnauthorized());
        send(get("/api/v1/payments/installment-options"), token, null)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.termsInMonths.length()").value(4))
                .andExpect(jsonPath("$.data.termsInMonths[0]").value(3))
                .andExpect(jsonPath("$.data.termsInMonths[3]").value(12))
                .andExpect(jsonPath("$.data.minOrderTotal").value(3000000))
                .andExpect(jsonPath("$.data.interestRate").value(0))
                .andExpect(jsonPath("$.data.citizenIdLength").value(10))
                .andExpect(jsonPath("$.data.banks.length()").value(10))
                .andExpect(jsonPath("$.data.banks[0].code").value("VCB"))
                .andExpect(jsonPath("$.data.banks[0].name").value("Vietcombank"))
                .andExpect(jsonPath("$.data.partners.length()").value(3))
                .andExpect(jsonPath("$.data.partners[0].code").value("CREDIT_CARD"))
                .andExpect(jsonPath("$.data.partners[0].requiresCardBank").value(true))
                .andExpect(jsonPath("$.data.partners[0].termsInMonths.length()").value(4))
                .andExpect(jsonPath("$.data.partners[1].code").value("FINANCE_A"))
                .andExpect(jsonPath("$.data.partners[1].requiresCardBank").value(false))
                .andExpect(jsonPath("$.data.partners[1].termsInMonths[0]").value(6))
                .andExpect(jsonPath("$.data.partners[2].maxOrderTotal").value(15000000))
                .andExpect(jsonPath("$.data.partners[0].feePercent").doesNotExist());
    }

    @Test
    void installmentOptions_withTheOrderTotal_onlyListThePartnersThatAcceptIt() throws Exception {
        send(get("/api/v1/payments/installment-options").param("total", "20000000"), token, null)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.partners.length()").value(2))
                .andExpect(jsonPath("$.data.partners[1].code").value("FINANCE_A"));
        send(get("/api/v1/payments/installment-options").param("total", "2000000"), token, null)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.partners.length()").value(0))
                .andExpect(jsonPath("$.data.termsInMonths.length()").value(0));
    }

    @Test
    void productSales_countsUnitsOfOrdersThatAreNotCancelled_andIsPublic() throws Exception {
        String ids = phone.getId() + "," + cover.getProduct().getId();
        mockMvc.perform(get("/api/v1/products/sales?ids=" + ids))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].sold").value(0))
                .andExpect(jsonPath("$.data[1].sold").value(0));

        addToCart(black, 2);
        addToCart(cover, 1);
        send(post("/api/v1/orders"), token, orderBody()).andExpect(status().isCreated());
        addToCart(white, 3);
        long cancelled = data(send(post("/api/v1/orders"), token, orderBody()).andExpect(status().isCreated())).get("id").asLong();
        send(post("/api/v1/orders/" + cancelled + "/cancel"), token, null).andExpect(status().isOk());

        mockMvc.perform(get("/api/v1/products/sales?ids=" + ids))
                .andExpect(jsonPath("$.data[0].productId").value(phone.getId()))
                .andExpect(jsonPath("$.data[0].sold").value(2))
                .andExpect(jsonPath("$.data[1].sold").value(1));

        mockMvc.perform(get("/api/v1/products/sales")).andExpect(status().isBadRequest());
        mockMvc.perform(get("/api/v1/products/sales?ids=1,abc")).andExpect(status().isBadRequest());
    }

    @Test
    void productStoreStock_listsOpenStoresWithStock_isPublic_and404ForUnknownProduct() throws Exception {
        String body = mockMvc.perform(get("/api/v1/products/" + phone.getId() + "/store-stock"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        JsonNode rows = jsonMapper.readTree(body).get("data");
        assertThat(rows.size()).isGreaterThan(0);
        for (JsonNode row : rows) {
            assertThat(row.get("quantity").asInt()).isPositive();
            assertThat(row.get("storeName").asString()).isNotBlank();
        }

        mockMvc.perform(get("/api/v1/products/999999999/store-stock")).andExpect(status().isNotFound());
    }

    @Test
    void topBrands_arePublic_sortedByProductCount_withoutProductTypeWords() throws Exception {
        String body = mockMvc.perform(get("/api/v1/brands/top?size=40"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        JsonNode brands = jsonMapper.readTree(body).get("data");
        long previous = Long.MAX_VALUE;
        for (JsonNode brand : brands) {
            assertThat(brand.get("productCount").asLong()).isLessThanOrEqualTo(previous);
            previous = brand.get("productCount").asLong();
            assertThat(brand.get("name").asString().toLowerCase()).isNotIn("túi", "ốp", "đèn", "camera", "usb", "miếng");
        }
        assertThat(brands.size()).isLessThanOrEqualTo(40);
    }

    @Test
    void placeOrder_keepsTheContactEmail_andRefusesABadOne() throws Exception {
        addToCart(cover, 1);
        Map<String, Object> bad = orderBody();
        bad.put("contactEmail", "không-phải-email");
        send(post("/api/v1/orders"), token, bad)
                .andExpect(status().isBadRequest());
        assertThat(orderRepository.findAllByUserId(userId, PageRequest.of(0, 10)).getTotalElements()).isZero();

        Map<String, Object> body = orderBody();
        body.put("contactEmail", "khach.nhan@example.com");
        long id = data(send(post("/api/v1/orders"), token, body).andExpect(status().isCreated())).get("id").asLong();
        assertThat(orderRepository.findById(id).orElseThrow().getContactEmail()).isEqualTo("khach.nhan@example.com");

        addToCart(cover, 1);
        long withoutEmail = data(send(post("/api/v1/orders"), token, orderBody()).andExpect(status().isCreated()))
                .get("id").asLong();
        assertThat(orderRepository.findById(withoutEmail).orElseThrow().getContactEmail()).isNull();
    }

    @Test
    void trackingLink_isPublic_showsOnlyTheProgress_andAnUnknownTokenIs404() throws Exception {
        addToCart(black, 1);
        long id = data(send(post("/api/v1/orders"), token, orderBody()).andExpect(status().isCreated())).get("id").asLong();
        String trackingToken = orderRepository.findById(id).orElseThrow().getTrackingToken();
        assertThat(trackingToken).matches("[0-9a-f]{64}");

        String body = mockMvc.perform(get("/api/v1/orders/track/" + trackingToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.code").value("DH%08d".formatted(id)))
                .andExpect(jsonPath("$.data.status").value("PENDING"))
                .andExpect(jsonPath("$.data.paymentMethod").value("COD"))
                .andExpect(jsonPath("$.data.items.length()").value(1))
                .andExpect(jsonPath("$.data.items[0].name").value("Điện thoại Đơn Hàng X - Đen 128GB"))
                .andExpect(jsonPath("$.data.total").value(14990000))
                .andExpect(jsonPath("$.data.steps.length()").value(1))
                .andReturn().getResponse().getContentAsString();
        assertThat(body).doesNotContain("0901").doesNotContain("Nguyễn Trãi").doesNotContain("Nguyễn Văn An")
                .doesNotContain("order.test");

        mockMvc.perform(get("/api/v1/orders/track/" + "0".repeat(64)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error.code").value("ORDER_NOT_FOUND"));
        mockMvc.perform(get("/api/v1/orders/track/" + id)).andExpect(status().isNotFound());
        mockMvc.perform(get("/api/v1/orders/track/ABC")).andExpect(status().isNotFound());
    }

    private JsonNode register(String username) throws Exception {
        JsonNode data = data(send(post("/api/v1/auth/register"), null, Map.of(
                "email", username + "@example.com", "username", username, "password", PASSWORD,
                "fullname", "Nguyễn Thị Đơn Hàng"))
                .andExpect(status().isCreated()));
        registeredUserIds.add(data.get("user").get("id").asLong());
        return data;
    }

    private void addToCart(ProductVariant variant, int quantity) throws Exception {
        send(post("/api/v1/cart/items"), token, Map.of("variantId", variant.getId(), "quantity", quantity))
                .andExpect(status().isOk());
    }

    private static Map<String, Object> orderBody() {
        return orderBody("COD");
    }

    private static Map<String, Object> orderBody(String paymentMethod) {
        Map<String, Object> body = new HashMap<>();
        body.put("recipientName", "  Nguyễn Văn An ");
        body.put("recipientPhone", "0901 234 567");
        body.put("shippingAddress", "12 Nguyễn Trãi, Phường 3, Quận 5, TP. Hồ Chí Minh");
        body.put("note", "Gọi trước khi giao");
        body.put("paymentMethod", paymentMethod);
        return body;
    }

    private static Map<String, Object> installmentBody(int months, String citizenId, String cardBank) {
        Map<String, Object> body = orderBody("INSTALLMENT");
        body.put("installment", new HashMap<>(Map.of("months", months, "citizenId", citizenId, "cardBank", cardBank)));
        return body;
    }

    private static Map<String, Object> partnerBody(String partner, int months, String cardBank) {
        Map<String, Object> installment = new HashMap<>(Map.of("months", months, "citizenId", "0123456789", "partner", partner));
        if (cardBank != null) {
            installment.put("cardBank", cardBank);
        }
        Map<String, Object> body = orderBody("INSTALLMENT");
        body.put("installment", installment);
        return body;
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

    private static Store store(String name, String district, boolean active) {
        Store store = new Store();
        store.setName(name);
        store.setAddress("1 Đường Test");
        store.setDistrict(district);
        store.setCity("Hồ Chí Minh");
        store.setActive(active);
        return store;
    }

    private static Product product(String name, String slug, Category category, String price) {
        Product product = new Product();
        product.setName(name);
        product.setSlug(slug);
        product.setCategory(category);
        product.setBasePrice(new BigDecimal(price));
        return product;
    }

    private static ProductVariant variant(Product product, String name, String price, String discountPrice) {
        ProductVariant variant = new ProductVariant();
        variant.setProduct(product);
        variant.setVariantName(name);
        variant.setPrice(new BigDecimal(price));
        variant.setDiscountPrice(discountPrice != null ? new BigDecimal(discountPrice) : null);
        return variant;
    }
}
