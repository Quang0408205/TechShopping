package com.example.Tech.controller.wishlist;

import com.example.Tech.entity.product.Category;
import com.example.Tech.entity.product.Product;
import com.example.Tech.entity.product.ProductVariant;
import com.example.Tech.entity.user.UserRole;
import com.example.Tech.entity.wishlist.WishlistItem;
import com.example.Tech.repository.mail.EmailLogRepository;
import com.example.Tech.repository.product.CategoryRepository;
import com.example.Tech.repository.product.ProductRepository;
import com.example.Tech.repository.product.ProductVariantRepository;
import com.example.Tech.repository.user.RoleRepository;
import com.example.Tech.repository.user.UserRepository;
import com.example.Tech.repository.user.UserRoleRepository;
import com.example.Tech.repository.wishlist.WishlistItemRepository;
import com.example.Tech.security.RefreshTokenService;
import com.example.Tech.service.mail.MailMessage;
import com.example.Tech.service.mail.MailTransport;
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
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** /api/v1/wishlist (save / list / remove) and the price-drop check + mail (ADMIN run). */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class WishlistApiIntegrationTest {

    private static final String PASSWORD = "Matkhau@123";

    @Autowired private MockMvc mockMvc;
    @Autowired private JsonMapper jsonMapper;
    @Autowired private RefreshTokenService refreshTokenService;
    @Autowired private CategoryRepository categoryRepository;
    @Autowired private ProductRepository productRepository;
    @Autowired private ProductVariantRepository variantRepository;
    @Autowired private UserRepository userRepository;
    @Autowired private RoleRepository roleRepository;
    @Autowired private UserRoleRepository userRoleRepository;
    @Autowired private WishlistItemRepository wishlistRepository;
    @Autowired private EmailLogRepository emailLogRepository;
    @Autowired private EntityManager entityManager;
    @MockitoBean private MailTransport mailTransport;

    private final List<Long> registeredUserIds = new ArrayList<>();

    private String admin;
    private String customer;
    private String other;
    private Long customerId;
    private Product phone;
    private Product tablet;

    @BeforeEach
    void setUp() throws Exception {
        admin = registerWithRole("wish.admin", "ADMIN");
        JsonNode auth = register("wish.khach");
        customer = auth.get("accessToken").asString();
        customerId = auth.get("user").get("id").asLong();
        other = register("wish.khac").get("accessToken").asString();
        Category category = new Category();
        category.setName("Danh mục yêu thích");
        category.setSlug("test-wishlist-category");
        category = categoryRepository.save(category);
        phone = product("Điện thoại Yêu Thích", "test-wishlist-phone", category, "20000000");
        tablet = product("Máy tính bảng Yêu Thích", "test-wishlist-tablet", category, "9000000");
        ProductVariant v1 = variant(phone, "Đen", "20000000");
        ProductVariant v2 = variant(tablet, "Xám", "9000000");
        StoreFixtures.warehouse(entityManager, v1, v2);
        entityManager.flush();
    }

    @AfterEach
    void revokeTokens() {
        registeredUserIds.forEach(refreshTokenService::revokeAll);
    }

    @Test
    void wishlist_requiresAToken() throws Exception {
        send(get("/api/v1/wishlist"), null, null).andExpect(status().isUnauthorized());
        send(get("/api/v1/wishlist/ids"), null, null).andExpect(status().isUnauthorized());
        send(post("/api/v1/wishlist/" + phone.getId()), null, null).andExpect(status().isUnauthorized());
        send(delete("/api/v1/wishlist/" + phone.getId()), null, null).andExpect(status().isUnauthorized());
    }

    @Test
    void save_list_remove_andEachCustomerSeesOnlyTheirOwn() throws Exception {
        send(post("/api/v1/wishlist/" + phone.getId()), customer, null).andExpect(status().isOk());
        send(post("/api/v1/wishlist/" + phone.getId()), customer, null).andExpect(status().isOk());   // again: no duplicate
        send(post("/api/v1/wishlist/" + tablet.getId()), customer, null).andExpect(status().isOk());
        assertThat(wishlistRepository.countByUserId(customerId)).isEqualTo(2);

        send(get("/api/v1/wishlist/ids"), customer, null)
                .andExpect(jsonPath("$.data.length()").value(2))
                .andExpect(jsonPath("$.data[0]").value(tablet.getId()))
                .andExpect(jsonPath("$.data[1]").value(phone.getId()));
        send(get("/api/v1/wishlist"), customer, null)
                .andExpect(jsonPath("$.data.length()").value(2))
                .andExpect(jsonPath("$.data[0].name").value("Máy tính bảng Yêu Thích"))
                .andExpect(jsonPath("$.data[1].price").value(20000000))
                .andExpect(jsonPath("$.data[1].priceAtAdd").value(20000000))
                .andExpect(jsonPath("$.data[1].inStock").value(true));
        send(get("/api/v1/wishlist"), other, null).andExpect(jsonPath("$.data.length()").value(0));

        send(delete("/api/v1/wishlist/" + phone.getId()), other, null).andExpect(status().isOk());   // not theirs: nothing happens
        assertThat(wishlistRepository.countByUserId(customerId)).isEqualTo(2);
        send(delete("/api/v1/wishlist/" + phone.getId()), customer, null).andExpect(status().isOk());
        send(delete("/api/v1/wishlist/" + phone.getId()), customer, null).andExpect(status().isOk());   // again: fine
        send(get("/api/v1/wishlist/ids"), customer, null).andExpect(jsonPath("$.data.length()").value(1));
    }

    @Test
    void save_unknownOrRemovedProduct_is404_andTheListSkipsAProductTakenOffSale() throws Exception {
        send(post("/api/v1/wishlist/999999999"), customer, null)
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error.code").value("PRODUCT_NOT_FOUND"));

        send(post("/api/v1/wishlist/" + phone.getId()), customer, null).andExpect(status().isOk());
        phone.setActive(false);
        productRepository.saveAndFlush(phone);
        send(get("/api/v1/wishlist"), customer, null).andExpect(jsonPath("$.data.length()").value(0));
        send(post("/api/v1/wishlist/" + phone.getId()), other, null).andExpect(status().isNotFound());
    }

    @Test
    void priceCheck_isAdminOnly_andMailsTheDropOnce_thenTracksTheNewMark() throws Exception {
        send(post("/api/v1/admin/wishlist/price-alerts/run"), customer, null).andExpect(status().isForbidden());
        send(post("/api/v1/admin/wishlist/price-alerts/run"), null, null).andExpect(status().isUnauthorized());

        send(post("/api/v1/wishlist/" + phone.getId()), customer, null).andExpect(status().isOk());
        send(post("/api/v1/wishlist/" + tablet.getId()), customer, null).andExpect(status().isOk());
        when(mailTransport.send(any())).thenReturn(true);

        // nothing cheaper yet: nothing is mailed
        send(post("/api/v1/admin/wishlist/price-alerts/run"), admin, null)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.dropped").value(0))
                .andExpect(jsonPath("$.data.mailed").value(0));
        verify(mailTransport, never()).send(any());

        // the phone gets 2.000.000đ cheaper: one mail to the customer with that product only
        phone.setDiscountPrice(new BigDecimal("18000000"));
        productRepository.saveAndFlush(phone);
        send(post("/api/v1/admin/wishlist/price-alerts/run"), admin, null)
                .andExpect(jsonPath("$.data.dropped").value(1))
                .andExpect(jsonPath("$.data.mailed").value(1));
        ArgumentCaptor<MailMessage> sent = ArgumentCaptor.forClass(MailMessage.class);
        verify(mailTransport).send(sent.capture());
        assertThat(sent.getValue().to()).isEqualTo("wish.khach@example.com");
        assertThat(sent.getValue().subject()).isEqualTo("[POY] Điện thoại Yêu Thích đang giảm giá");
        assertThat(sent.getValue().text()).contains("20.000.000đ").contains("18.000.000đ").contains("giảm 10%")
                .contains("product-detail.html?id=" + phone.getId()).doesNotContain("Máy tính bảng");
        assertThat(sent.getValue().html()).contains("Giảm 10%");
        assertThat(emailLogRepository.findAll()).anyMatch(log -> "WISHLIST_PRICE_DROP".equals(log.getKind())
                && "wish.khach@example.com".equals(log.getRecipient()) && "SENT".equals(log.getStatus()));
        send(get("/api/v1/wishlist"), customer, null)
                .andExpect(jsonPath("$.data[1].price").value(18000000))
                .andExpect(jsonPath("$.data[1].priceAtAdd").value(20000000));

        // running again with the same price: no second mail
        send(post("/api/v1/admin/wishlist/price-alerts/run"), admin, null).andExpect(jsonPath("$.data.dropped").value(0));
        verify(mailTransport).send(any());

        // the price goes up: the mark follows, so a later drop is reported again
        phone.setDiscountPrice(new BigDecimal("19500000"));
        productRepository.saveAndFlush(phone);
        send(post("/api/v1/admin/wishlist/price-alerts/run"), admin, null).andExpect(jsonPath("$.data.dropped").value(0));
        WishlistItem mark = wishlistRepository.findByUserIdAndProductId(customerId, phone.getId()).orElseThrow();
        assertThat(mark.getLastAlertPrice()).isEqualByComparingTo("19500000");
        phone.setDiscountPrice(new BigDecimal("19000000"));
        productRepository.saveAndFlush(phone);
        send(post("/api/v1/admin/wishlist/price-alerts/run"), admin, null).andExpect(jsonPath("$.data.mailed").value(1));
        verify(mailTransport, org.mockito.Mockito.times(2)).send(any());
    }

    // ---------- helpers ----------

    private Product product(String name, String slug, Category category, String price) {
        Product product = new Product();
        product.setName(name);
        product.setSlug(slug);
        product.setCategory(category);
        product.setBasePrice(new BigDecimal(price));
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
                "fullname", "Người Thử Yêu Thích"))
                .andExpect(status().isCreated()));
        registeredUserIds.add(data.get("user").get("id").asLong());
        return data;
    }

    private String registerWithRole(String username, String role) throws Exception {
        Long id = register(username).get("user").get("id").asLong();
        userRoleRepository.save(new UserRole(userRepository.getReferenceById(id), roleRepository.findByName(role).orElseThrow()));
        entityManager.flush();
        return data(send(post("/api/v1/auth/login"), null, Map.of("identifier", username, "password", PASSWORD))
                .andExpect(status().isOk())).get("accessToken").asString();
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
