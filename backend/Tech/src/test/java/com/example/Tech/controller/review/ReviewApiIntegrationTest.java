package com.example.Tech.controller.review;

import com.example.Tech.entity.order.Order;
import com.example.Tech.entity.order.OrderItem;
import com.example.Tech.entity.order.OrderStatus;
import com.example.Tech.entity.order.PaymentMethod;
import com.example.Tech.entity.product.Category;
import com.example.Tech.entity.product.Product;
import com.example.Tech.entity.product.ProductVariant;
import com.example.Tech.entity.review.Review;
import com.example.Tech.repository.order.OrderRepository;
import com.example.Tech.repository.product.CategoryRepository;
import com.example.Tech.repository.product.ProductRepository;
import com.example.Tech.repository.product.ProductVariantRepository;
import com.example.Tech.repository.review.ReviewRepository;
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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Product reviews over HTTP against the test database (rolled back): public reads, writing with an uploaded photo,
 * one review per account, "Đã mua hàng" from a DELIVERED order, the product's rating kept in step on every write,
 * edits / deletes limited to the author, hidden reviews left out of the public figures. Uploaded photo files stay in
 * the test upload folder (the test transaction never commits, so nothing is deleted after commit).
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class ReviewApiIntegrationTest {

    private static final String PASSWORD = "Matkhau@123";
    private static final byte[] JPEG = Arrays.copyOf(new byte[]{(byte) 0xFF, (byte) 0xD8, (byte) 0xFF, (byte) 0xE0}, 64);
    private static final String COMMENT = "Máy chạy mượt, pin dùng cả ngày.";

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
    private ProductRepository productRepository;

    @Autowired
    private ProductVariantRepository variantRepository;

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private ReviewRepository reviewRepository;

    @Autowired
    private EntityManager entityManager;

    private final List<Long> registeredUserIds = new ArrayList<>();

    private String tokenA;
    private String tokenB;
    private Product product;
    private Product hiddenProduct;

    @BeforeEach
    void setUp() throws Exception {
        tokenA = register("rv.khacha", "Khách Hàng A");
        tokenB = register("rv.khachb", "Khách Hàng B");

        Category category = new Category();
        category.setName("Đánh giá test");
        category.setSlug("test-rv-category");
        category = categoryRepository.save(category);
        product = productRepository.save(product("Tai nghe RV", "test-rv-product", category, true));
        hiddenProduct = productRepository.save(product("Tai nghe RV ẩn", "test-rv-hidden", category, false));
        ProductVariant variant = new ProductVariant();
        variant.setProduct(product);
        variant.setVariantName("Mặc định");
        variant.setPrice(new BigDecimal("500000"));
        variant = variantRepository.save(variant);

        // Khách B already received this product
        Order order = new Order();
        order.setUser(userRepository.findByUsername("rv.khachb").orElseThrow());
        order.setRecipientName("Khách Hàng B");
        order.setRecipientPhone("0901234567");
        order.setShippingAddress("1 Lê Lợi, Quận 1, Hồ Chí Minh");
        order.setTotalAmount(new BigDecimal("500000"));
        order.setStatus(OrderStatus.DELIVERED);
        order.setPaymentMethod(PaymentMethod.COD);
        OrderItem item = new OrderItem();
        item.setVariant(variant);
        item.setQuantity(1);
        item.setUnitPrice(new BigDecimal("500000"));
        item.setSubtotal(new BigDecimal("500000"));
        order.addItem(item);
        orderRepository.save(order);
        entityManager.flush();
    }

    @AfterEach
    void revokeTokens() {
        registeredUserIds.forEach(refreshTokenService::revokeAll);
    }

    @Test
    void publicReads_andWhatNeedsAnAccount() throws Exception {
        send(get(listUrl()), null, null).andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalElements").value(0));
        send(get(listUrl() + "/summary"), null, null).andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalReviews").value(0))
                .andExpect(jsonPath("$.data.averageRating").value(0))
                .andExpect(jsonPath("$.data.stars.length()").value(5));
        send(get("/api/v1/products/-1/reviews"), null, null).andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error.code").value("PRODUCT_NOT_FOUND"));
        send(post("/api/v1/reviews"), null, body(product, 5, COMMENT, null)).andExpect(status().isUnauthorized());
        send(get("/api/v1/reviews/mine"), null, null).andExpect(status().isUnauthorized());
        mockMvc.perform(multipart("/api/v1/uploads/review-images").file(photoPart()))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void writeWithAPhoto_verifiedBuyer_ratingAndSummaryFollow_andFilters() throws Exception {
        String photo = upload(tokenA);
        assertThat(photo).contains("/uploads/reviews/");
        JsonNode written = data(send(post("/api/v1/reviews"), tokenA, body(product, 4, "  " + COMMENT + "  ", List.of(photo)))
                .andExpect(status().isCreated()));
        assertThat(written.get("comment").asString()).isEqualTo(COMMENT);
        assertThat(written.get("authorName").asString()).isEqualTo("Khách Hàng A");
        assertThat(written.get("verifiedPurchase").asBoolean()).isFalse();
        data(send(post("/api/v1/reviews"), tokenB, body(product, 2, "Âm thanh hơi nhỏ so với giá.", null))
                .andExpect(status().isCreated()));

        Product saved = productRepository.findById(product.getId()).orElseThrow();
        assertThat(saved.getRating()).isEqualByComparingTo("3.00");
        assertThat(saved.getTotalReviews()).isEqualTo(2);
        send(get("/api/v1/products/" + product.getId()), null, null)
                .andExpect(jsonPath("$.data.rating").value(3.0))
                .andExpect(jsonPath("$.data.totalReviews").value(2));

        JsonNode list = data(send(get(listUrl()), null, null).andExpect(status().isOk())).get("content");
        assertThat(list).hasSize(2);
        assertThat(list.get(0).get("authorName").asString()).isEqualTo("Khách Hàng B");
        assertThat(list.get(0).get("verifiedPurchase").asBoolean()).isTrue();
        assertThat(list.get(1).get("imageUrls").get(0).asString()).isEqualTo(photo);
        assertThat(list.get(1).get("hiddenReason").isNull()).isTrue();

        JsonNode summary = data(send(get(listUrl() + "/summary"), null, null));
        assertThat(summary.get("averageRating").asDouble()).isEqualTo(3.0);
        assertThat(summary.get("withImages").asLong()).isEqualTo(1);
        List<Long> counts = new ArrayList<>();
        summary.get("stars").forEach(row -> counts.add(row.get("count").asLong()));
        assertThat(counts).containsExactly(0L, 1L, 0L, 1L, 0L);

        assertThat(authors(listUrl() + "?rating=4")).containsExactly("Khách Hàng A");
        assertThat(authors(listUrl() + "?withImages=true")).containsExactly("Khách Hàng A");
        JsonNode mine = data(send(get("/api/v1/reviews/mine").param("productId", String.valueOf(product.getId())),
                tokenA, null).andExpect(status().isOk()));
        assertThat(mine).hasSize(1);
        assertThat(mine.get(0).get("productName").asString()).isEqualTo("Tai nghe RV");
    }

    @Test
    void invalidReviews() throws Exception {
        send(post("/api/v1/reviews"), tokenA, body(product, 5, COMMENT, null)).andExpect(status().isCreated());
        send(post("/api/v1/reviews"), tokenA, body(product, 3, COMMENT, null))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error.code").value("ALREADY_REVIEWED"));
        send(post("/api/v1/reviews"), tokenB, body(product, 5, COMMENT, List.of("https://evil.example/a.jpg")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.details.imageUrls").exists());
        send(post("/api/v1/reviews"), tokenB, body(product, 5, COMMENT,
                List.of("http://localhost:8080/uploads/products/11111111-1111-1111-1111-111111111111.jpg")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.details.imageUrls").exists());
        send(post("/api/v1/reviews"), tokenB, body(product, 6, COMMENT, null))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.details.rating").exists());
        send(post("/api/v1/reviews"), tokenB, body(product, 5, "  ngắn  ", null))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.details.comment").exists());
        Map<String, Object> noProduct = body(product, 5, COMMENT, null);
        noProduct.remove("productId");
        send(post("/api/v1/reviews"), tokenB, noProduct).andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.details.productId").exists());
        send(post("/api/v1/reviews"), tokenB, body(hiddenProduct, 5, COMMENT, null))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error.code").value("PRODUCT_NOT_AVAILABLE"));
        mockMvc.perform(multipart("/api/v1/uploads/review-images")
                        .file(new MockMultipartFile("file", "x.jpg", "image/jpeg", "<svg/>".getBytes()))
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenB))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("INVALID_IMAGE_FILE"));
        assertThat(productRepository.findById(product.getId()).orElseThrow().getTotalReviews()).isEqualTo(1);
    }

    @Test
    void onlyTheAuthorEditsOrDeletes_thenTheProductCanBeReviewedAgain() throws Exception {
        long idA = data(send(post("/api/v1/reviews"), tokenA, body(product, 4, COMMENT, List.of(upload(tokenA))))
                .andExpect(status().isCreated())).get("id").asLong();
        send(post("/api/v1/reviews"), tokenB, body(product, 2, COMMENT, null)).andExpect(status().isCreated());

        send(put("/api/v1/reviews/" + idA), tokenB, body(product, 1, COMMENT, null))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error.code").value("REVIEW_NOT_FOUND"));
        send(delete("/api/v1/reviews/" + idA), tokenB, null).andExpect(status().isNotFound());

        JsonNode edited = data(send(put("/api/v1/reviews/" + idA), tokenA, body(product, 5, COMMENT + " Sửa.", null))
                .andExpect(status().isOk()));
        assertThat(edited.get("edited").asBoolean()).isTrue();
        assertThat(edited.get("imageUrls")).isEmpty();
        assertThat(productRepository.findById(product.getId()).orElseThrow().getRating()).isEqualByComparingTo("3.50");

        send(delete("/api/v1/reviews/" + idA), tokenA, null).andExpect(status().isNoContent());
        Product afterDelete = productRepository.findById(product.getId()).orElseThrow();
        assertThat(afterDelete.getTotalReviews()).isEqualTo(1);
        assertThat(afterDelete.getRating()).isEqualByComparingTo("2.00");
        send(post("/api/v1/reviews"), tokenA, body(product, 3, COMMENT, null)).andExpect(status().isCreated());
    }

    @Test
    void aHiddenReview_staysOutOfThePublicFigures_butItsAuthorSeesWhy() throws Exception {
        long idA = data(send(post("/api/v1/reviews"), tokenA, body(product, 1, COMMENT, null))
                .andExpect(status().isCreated())).get("id").asLong();
        send(post("/api/v1/reviews"), tokenB, body(product, 5, COMMENT, null)).andExpect(status().isCreated());
        Review hidden = reviewRepository.findById(idA).orElseThrow();
        hidden.setHidden(true);
        hidden.setHiddenReason("Nội dung không liên quan");
        entityManager.flush();

        JsonNode edited = data(send(put("/api/v1/reviews/" + idA), tokenA, body(product, 2, COMMENT + " Sửa.", null))
                .andExpect(status().isOk()));
        assertThat(edited.get("hidden").asBoolean()).isTrue();
        assertThat(authors(listUrl())).containsExactly("Khách Hàng B");
        assertThat(data(send(get(listUrl() + "/summary"), null, null)).get("totalReviews").asLong()).isEqualTo(1);
        Product saved = productRepository.findById(product.getId()).orElseThrow();
        assertThat(saved.getTotalReviews()).isEqualTo(1);
        assertThat(saved.getRating()).isEqualByComparingTo("5.00");
        JsonNode mine = data(send(get("/api/v1/reviews/mine"), tokenA, null).andExpect(status().isOk()));
        assertThat(mine.get(0).get("hiddenReason").asString()).isEqualTo("Nội dung không liên quan");
    }

    private String listUrl() {
        return "/api/v1/products/" + product.getId() + "/reviews";
    }

    private List<String> authors(String url) throws Exception {
        List<String> names = new ArrayList<>();
        data(send(get(url), null, null).andExpect(status().isOk())).get("content")
                .forEach(row -> names.add(row.get("authorName").asString()));
        return names;
    }

    private String upload(String token) throws Exception {
        return data(mockMvc.perform(multipart("/api/v1/uploads/review-images").file(photoPart())
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isCreated())).get("url").asString();
    }

    private static MockMultipartFile photoPart() {
        return new MockMultipartFile("file", "anh.jpg", "image/jpeg", JPEG);
    }

    private static Map<String, Object> body(Product target, int rating, String comment, List<String> imageUrls) {
        Map<String, Object> body = new HashMap<>();
        body.put("productId", target.getId());
        body.put("rating", rating);
        body.put("comment", comment);
        if (imageUrls != null) {
            body.put("imageUrls", imageUrls);
        }
        return body;
    }

    private static Product product(String name, String slug, Category category, boolean active) {
        Product product = new Product();
        product.setName(name);
        product.setSlug(slug);
        product.setCategory(category);
        product.setBasePrice(new BigDecimal("500000"));
        product.setActive(active);
        return product;
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
