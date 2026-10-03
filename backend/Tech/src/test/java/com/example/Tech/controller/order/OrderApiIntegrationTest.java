package com.example.Tech.controller.order;

import com.example.Tech.entity.product.Category;
import com.example.Tech.entity.product.Product;
import com.example.Tech.entity.product.ProductImage;
import com.example.Tech.entity.product.ProductVariant;
import com.example.Tech.repository.order.OrderRepository;
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
                .andExpect(jsonPath("$.error.details.shippingAddress").exists())
                .andExpect(jsonPath("$.error.details.paymentMethod").exists());
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
        Map<String, Object> body = new HashMap<>();
        body.put("recipientName", "  Nguyễn Văn An ");
        body.put("recipientPhone", "0901 234 567");
        body.put("shippingAddress", "12 Nguyễn Trãi, Phường 3, Quận 5, TP. Hồ Chí Minh");
        body.put("note", "Gọi trước khi giao");
        body.put("paymentMethod", "COD");
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
