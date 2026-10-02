package com.example.Tech.controller.cart;

import com.example.Tech.entity.product.Brand;
import com.example.Tech.entity.product.Category;
import com.example.Tech.entity.product.Product;
import com.example.Tech.entity.product.ProductImage;
import com.example.Tech.entity.product.ProductVariant;
import com.example.Tech.entity.user.User;
import com.example.Tech.repository.cart.CartItemRepository;
import com.example.Tech.repository.cart.CartRepository;
import com.example.Tech.repository.product.BrandRepository;
import com.example.Tech.repository.product.CategoryRepository;
import com.example.Tech.repository.product.ProductImageRepository;
import com.example.Tech.repository.product.ProductRepository;
import com.example.Tech.repository.product.ProductVariantRepository;
import com.example.Tech.repository.user.UserRepository;
import com.example.Tech.security.RefreshTokenService;
import com.example.Tech.service.product.ProductService;
import com.example.Tech.service.product.ProductVariantService;
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

/**
 * /api/v1/cart over HTTP against the test database (rolled back) and the real Redis (all refresh tokens of
 * the registered users are revoked afterwards). The test DB has no catalogue, so each test creates its own.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class CartApiIntegrationTest {

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
    private CategoryRepository categoryRepository;

    @Autowired
    private BrandRepository brandRepository;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private ProductVariantRepository variantRepository;

    @Autowired
    private ProductImageRepository imageRepository;

    @Autowired
    private CartRepository cartRepository;

    @Autowired
    private CartItemRepository cartItemRepository;

    @Autowired
    private ProductService productService;

    @Autowired
    private ProductVariantService productVariantService;

    @Autowired
    private EntityManager entityManager;

    private final List<Long> registeredUserIds = new ArrayList<>();

    private Long userId;
    private String accessToken;
    private Product phone;
    private ProductVariant black;
    private ProductVariant white;
    private ProductVariant free;

    @BeforeEach
    void setUp() throws Exception {
        JsonNode auth = register("cart.test");
        userId = auth.get("user").get("id").asLong();
        accessToken = auth.get("accessToken").asString();

        Category category = new Category();
        category.setName("Điện thoại");
        category.setSlug("test-cart-api-dien-thoai");
        category = categoryRepository.save(category);
        Brand brand = new Brand();
        brand.setName("Test Cart API Brand");
        brand.setSlug("test-cart-api-brand");
        brand = brandRepository.save(brand);

        phone = productRepository.save(product("Điện thoại Giỏ Hàng X", "test-cart-api-x", category, brand, "15990000"));
        black = variantRepository.save(variant(phone, "Đen 128GB", "15990000", "14990000"));
        white = variantRepository.save(variant(phone, "Trắng 256GB", "18990000", null));
        Product charger = productRepository.save(product("Sạc Liên Hệ", "test-cart-api-sac", category, brand, "0"));
        free = variantRepository.save(variant(charger, "Mặc định", "0", null));

        ProductImage second = new ProductImage();
        second.setProduct(phone);
        second.setImageUrl("https://cdn.example/x-2.jpg");
        second.setDisplayOrder(1);
        imageRepository.save(second);
        ProductImage primary = new ProductImage();
        primary.setProduct(phone);
        primary.setImageUrl("https://cdn.example/x-primary.jpg");
        primary.setDisplayOrder(2);
        primary.setPrimary(true);
        imageRepository.save(primary);
        entityManager.flush();
    }

    @AfterEach
    void revokeTokens() {
        registeredUserIds.forEach(refreshTokenService::revokeAll);
    }

    private JsonNode register(String username) throws Exception {
        JsonNode data = data(send(post("/api/v1/auth/register"), null, Map.of(
                "email", username + "@example.com", "username", username, "password", PASSWORD,
                "fullname", "Nguyễn Thị Giỏ Hàng"))
                .andExpect(status().isCreated()));
        registeredUserIds.add(data.get("user").get("id").asLong());
        return data;
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

    private ResultActions add(ProductVariant variant, int quantity) throws Exception {
        return send(post("/api/v1/cart/items"), accessToken,
                Map.of("variantId", variant.getId(), "quantity", quantity));
    }

    @Test
    void cart_requiresToken() throws Exception {
        send(get("/api/v1/cart"), null, null)
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error.code").value("UNAUTHORIZED"));
        send(post("/api/v1/cart/items"), null, Map.of("variantId", black.getId(), "quantity", 1))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void newUser_hasAnEmptyCart_andNoCartRowIsCreated() throws Exception {
        send(get("/api/v1/cart"), accessToken, null)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.items").isEmpty())
                .andExpect(jsonPath("$.data.totalQuantity").value(0))
                .andExpect(jsonPath("$.data.subtotal").value(0))
                .andExpect(jsonPath("$.data.updatedAt").doesNotExist());
        assertThat(cartRepository.findByUserId(userId)).isEmpty();
    }

    @Test
    void add_usesTheServerPrice_mergesTheLine_andCapsItAtTen() throws Exception {
        add(black, 2)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.items.length()").value(1))
                .andExpect(jsonPath("$.data.items[0].variantId").value(black.getId()))
                .andExpect(jsonPath("$.data.items[0].productId").value(phone.getId()))
                .andExpect(jsonPath("$.data.items[0].productName").value("Điện thoại Giỏ Hàng X"))
                .andExpect(jsonPath("$.data.items[0].variantName").value("Đen 128GB"))
                .andExpect(jsonPath("$.data.items[0].imageUrl").value("https://cdn.example/x-primary.jpg"))
                .andExpect(jsonPath("$.data.items[0].unitPrice").value(14990000))
                .andExpect(jsonPath("$.data.items[0].originalPrice").value(15990000))
                .andExpect(jsonPath("$.data.items[0].lineTotal").value(29980000))
                .andExpect(jsonPath("$.data.items[0].available").value(true))
                .andExpect(jsonPath("$.data.subtotal").value(29980000))
                .andExpect(jsonPath("$.data.updatedAt").exists());

        add(black, 3).andExpect(jsonPath("$.data.items[0].quantity").value(5));
        add(white, 1)
                .andExpect(jsonPath("$.data.items.length()").value(2))
                .andExpect(jsonPath("$.data.items[1].originalPrice").doesNotExist())
                .andExpect(jsonPath("$.data.totalQuantity").value(6))
                .andExpect(jsonPath("$.data.subtotal").value(5 * 14990000L + 18990000L));
        add(black, 9).andExpect(jsonPath("$.data.items[0].quantity").value(10));

        // price and body fields other than variantId / quantity are ignored
        send(post("/api/v1/cart/items"), accessToken,
                Map.of("variantId", white.getId(), "quantity", 1, "price", 1))
                .andExpect(jsonPath("$.data.items[1].unitPrice").value(18990000));
    }

    @Test
    void add_rejectsInvalidInput() throws Exception {
        send(post("/api/v1/cart/items"), accessToken, Map.of("quantity", 1))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.error.details.variantId").exists());
        add(black, 0)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.details.quantity").exists());
        add(black, 11)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.details.quantity").exists());
        send(post("/api/v1/cart/items"), accessToken, Map.of("variantId", 999999999, "quantity", 1))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error.code").value("PRODUCT_VARIANT_NOT_FOUND"));
        add(free, 1)
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error.code").value("PRODUCT_NOT_AVAILABLE"));

        phone.setActive(false);
        productRepository.saveAndFlush(phone);
        add(black, 1)
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error.code").value("PRODUCT_NOT_AVAILABLE"));
        assertThat(cartRepository.findByUserId(userId)).isEmpty();
    }

    @Test
    void updateRemoveAndClear() throws Exception {
        add(black, 1);
        add(white, 1);

        send(put("/api/v1/cart/items/" + white.getId()), accessToken, Map.of("quantity", 4))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.items[1].quantity").value(4))
                .andExpect(jsonPath("$.data.totalQuantity").value(5));
        send(put("/api/v1/cart/items/" + white.getId()), accessToken, Map.of("quantity", 0))
                .andExpect(status().isBadRequest());
        send(put("/api/v1/cart/items/" + free.getId()), accessToken, Map.of("quantity", 2))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error.code").value("CART_ITEM_NOT_FOUND"));

        send(delete("/api/v1/cart/items/" + black.getId()), accessToken, null)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.items.length()").value(1))
                .andExpect(jsonPath("$.data.items[0].variantId").value(white.getId()));
        send(delete("/api/v1/cart/items/" + black.getId()), accessToken, null)
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error.code").value("CART_ITEM_NOT_FOUND"));

        send(delete("/api/v1/cart"), accessToken, null)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.items").isEmpty())
                .andExpect(jsonPath("$.data.subtotal").value(0));
        send(get("/api/v1/cart"), accessToken, null)
                .andExpect(jsonPath("$.data.items").isEmpty());
    }

    @Test
    void cartsAreSeparatePerUser() throws Exception {
        add(black, 2);
        String otherToken = register("cart.other").get("accessToken").asString();

        send(get("/api/v1/cart"), otherToken, null)
                .andExpect(jsonPath("$.data.items").isEmpty());
        send(delete("/api/v1/cart/items/" + black.getId()), otherToken, null)
                .andExpect(status().isNotFound());
        send(get("/api/v1/cart"), accessToken, null)
                .andExpect(jsonPath("$.data.items[0].quantity").value(2));
    }

    @Test
    void deletedProduct_staysInTheCartAsUnavailable_andIsNotCounted() throws Exception {
        add(black, 1);
        add(white, 1);

        productService.delete(phone.getId());
        entityManager.flush();
        entityManager.clear();

        send(get("/api/v1/cart"), accessToken, null)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.items.length()").value(2))
                .andExpect(jsonPath("$.data.items[0].available").value(false))
                .andExpect(jsonPath("$.data.hasUnavailableItems").value(true))
                .andExpect(jsonPath("$.data.totalQuantity").value(2))
                .andExpect(jsonPath("$.data.subtotal").value(0));
        // it can still be removed
        send(delete("/api/v1/cart/items/" + black.getId()), accessToken, null)
                .andExpect(status().isOk());
    }

    @Test
    void deletingAVariant_removesItFromCarts() throws Exception {
        add(black, 1);
        add(white, 1);

        productVariantService.delete(black.getId());
        entityManager.flush();

        assertThat(variantRepository.findById(black.getId())).isEmpty();
        send(get("/api/v1/cart"), accessToken, null)
                .andExpect(jsonPath("$.data.items.length()").value(1))
                .andExpect(jsonPath("$.data.items[0].variantId").value(white.getId()));
    }

    @Test
    void disabledAccount_isRejectedEvenWithAValidAccessToken() throws Exception {
        add(black, 1);
        User user = userRepository.findById(userId).orElseThrow();
        user.setActive(false);
        userRepository.saveAndFlush(user);

        send(get("/api/v1/cart"), accessToken, null)
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error.code").value("ACCOUNT_DISABLED"));
        add(white, 1)
                .andExpect(status().isForbidden());
        assertThat(cartItemRepository.countByCartId(cartRepository.findByUserId(userId).orElseThrow().getId()))
                .isEqualTo(1);
    }

    private static Product product(String name, String slug, Category category, Brand brand, String price) {
        Product product = new Product();
        product.setName(name);
        product.setSlug(slug);
        product.setCategory(category);
        product.setBrand(brand);
        product.setBasePrice(new BigDecimal(price));
        return product;
    }

    private static ProductVariant variant(Product product, String name, String price, String discountPrice) {
        ProductVariant variant = new ProductVariant();
        variant.setProduct(product);
        variant.setVariantName(name);
        variant.setPrice(new BigDecimal(price));
        variant.setDiscountPrice(discountPrice == null ? null : new BigDecimal(discountPrice));
        return variant;
    }
}
