package com.example.Tech.controller.stock;

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
import org.springframework.jdbc.core.JdbcTemplate;
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
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** "Tell me when it is back" over HTTP against the test database (rolled back), with the mail transport mocked. */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class StockSubscriptionApiIntegrationTest {

    private static final String PASSWORD = "Matkhau@123";
    private static final String URL = "/api/v1/stock-subscriptions";

    @Autowired private MockMvc mockMvc;
    @Autowired private JsonMapper jsonMapper;
    @Autowired private RefreshTokenService refreshTokenService;
    @Autowired private JdbcTemplate jdbc;
    @Autowired private UserRepository userRepository;
    @Autowired private RoleRepository roleRepository;
    @Autowired private UserRoleRepository userRoleRepository;
    @Autowired private CategoryRepository categoryRepository;
    @Autowired private ProductRepository productRepository;
    @Autowired private ProductVariantRepository variantRepository;
    @Autowired private EntityManager entityManager;
    @MockitoBean private MailTransport mailTransport;

    private final List<Long> registeredUserIds = new ArrayList<>();

    private String customerToken;
    private String otherToken;
    private String adminToken;
    private Product product;
    private ProductVariant variant;
    private ProductVariant second;
    private Store store;

    @BeforeEach
    void setUp() throws Exception {
        jdbc.update("delete from stock_subscriptions");
        customerToken = register("sa.customer").get("accessToken").asString();
        otherToken = register("sa.other").get("accessToken").asString();
        adminToken = registerAdmin("sa.admin");
        Category category = new Category();
        category.setName("Báo hàng test");
        category.setSlug("test-sa-category");
        category = categoryRepository.save(category);
        product = new Product();
        product.setName("Điện thoại Báo Hàng");
        product.setSlug("test-sa-phone");
        product.setCategory(category);
        product.setBasePrice(new BigDecimal("1000000"));
        product = productRepository.save(product);
        variant = variantRepository.save(variant(product, "Đen 128GB", "TK-SA-1"));
        second = variantRepository.save(variant(product, "Trắng 256GB", "TK-SA-2"));
        store = StoreFixtures.store(entityManager, "ZZ Kho báo hàng", "Quận Báo Hàng");
        entityManager.flush();
    }

    @AfterEach
    void revokeTokens() {
        registeredUserIds.forEach(refreshTokenService::revokeAll);
    }

    @Test
    void subscribing_needsALogin_anOutOfStockVariantOnSale_andIsIdempotent() throws Exception {
        send(post(URL), null, Map.of("variantId", variant.getId())).andExpect(status().isUnauthorized());
        send(get(URL), null, null).andExpect(status().isUnauthorized());
        send(post(URL), customerToken, Map.of()).andExpect(status().isBadRequest());
        send(post(URL), customerToken, Map.of("variantId", 999999999L)).andExpect(status().isNotFound());

        send(post(URL), customerToken, Map.of("variantId", variant.getId())).andExpect(status().isOk());
        send(post(URL), customerToken, Map.of("variantId", variant.getId())).andExpect(status().isOk());
        assertThat(ids(customerToken)).containsExactly(variant.getId());
        assertThat(ids(otherToken)).isEmpty();
        assertThat(jdbc.queryForObject("select count(*) from stock_subscriptions", Integer.class)).isEqualTo(1);

        StoreFixtures.stock(entityManager, store, second, 3);
        send(post(URL), customerToken, Map.of("variantId", second.getId())).andExpect(status().isConflict());

        send(delete(URL + "/" + variant.getId()), customerToken, null).andExpect(status().isOk());
        send(delete(URL + "/" + variant.getId()), customerToken, null).andExpect(status().isOk());
        assertThat(ids(customerToken)).isEmpty();
    }

    @Test
    void aClosedStoresStockDoesNotCount_andAnInactiveProductCannotBeFollowed() throws Exception {
        StoreFixtures.stock(entityManager, store, variant, 5);
        store.setActive(false);
        entityManager.merge(store);
        entityManager.flush();
        send(post(URL), customerToken, Map.of("variantId", variant.getId())).andExpect(status().isOk());

        product.setActive(false);
        productRepository.saveAndFlush(product);
        send(post(URL), customerToken, Map.of("variantId", second.getId())).andExpect(status().isNotFound());
    }

    @Test
    void theCheckIsAdminOnly_andMailsEachCustomerOnceWhenTheVariantIsBack() throws Exception {
        send(post("/api/v1/admin/stock-alerts/run"), customerToken, null).andExpect(status().isForbidden());
        send(post("/api/v1/admin/stock-alerts/run"), null, null).andExpect(status().isUnauthorized());

        send(post(URL), customerToken, Map.of("variantId", variant.getId())).andExpect(status().isOk());
        send(post(URL), customerToken, Map.of("variantId", second.getId())).andExpect(status().isOk());
        int loggedBefore = jdbc.queryForObject("select count(*) from email_log where kind = 'STOCK_AVAILABLE' and status = 'SENT'", Integer.class);
        send(post(URL), otherToken, Map.of("variantId", variant.getId())).andExpect(status().isOk());
        when(mailTransport.send(any())).thenReturn(true);

        send(post("/api/v1/admin/stock-alerts/run"), adminToken, null).andExpect(status().isOk())
                .andReturn();
        verify(mailTransport, never()).send(any());
        assertThat(jdbc.queryForObject("select count(*) from stock_subscriptions", Integer.class)).isEqualTo(3);

        StoreFixtures.stock(entityManager, store, variant, 4);
        StoreFixtures.stock(entityManager, store, second, 2);
        JsonNode first = data(send(post("/api/v1/admin/stock-alerts/run"), adminToken, null).andExpect(status().isOk()));
        assertThat(first.get("backInStock").asInt()).isEqualTo(3);
        assertThat(first.get("mailed").asInt()).isEqualTo(2);

        ArgumentCaptor<MailMessage> sent = ArgumentCaptor.forClass(MailMessage.class);
        verify(mailTransport, org.mockito.Mockito.times(2)).send(sent.capture());
        MailMessage toCustomer = sent.getAllValues().stream().filter(m -> m.to().equals("sa.customer@example.com")).findFirst().orElseThrow();
        assertThat(toCustomer.subject()).isEqualTo("[POY] 2 sản phẩm bạn quan tâm đã có hàng trở lại");
        assertThat(toCustomer.text()).contains("Điện thoại Báo Hàng - Đen 128GB").contains("Điện thoại Báo Hàng - Trắng 256GB")
                .contains("/customer/product-detail.html?id=" + product.getId());
        MailMessage toOther = sent.getAllValues().stream().filter(m -> m.to().equals("sa.other@example.com")).findFirst().orElseThrow();
        assertThat(toOther.subject()).isEqualTo("[POY] Điện thoại Báo Hàng - Đen 128GB đã có hàng trở lại");

        assertThat(jdbc.queryForObject("select count(*) from stock_subscriptions", Integer.class)).isZero();
        // the mail log is written in its own transaction and survives the rollback: count only what this test added
        assertThat(jdbc.queryForObject("select count(*) from email_log where kind = 'STOCK_AVAILABLE' and status = 'SENT'", Integer.class) - loggedBefore).isEqualTo(2);
        JsonNode again = data(send(post("/api/v1/admin/stock-alerts/run"), adminToken, null).andExpect(status().isOk()));
        assertThat(again.get("mailed").asInt()).isZero();
        verify(mailTransport, org.mockito.Mockito.times(2)).send(any());
    }

    @Test
    void aSubscriptionIsKeptWhenTheMailCouldNotBeSent_andDroppedWhenTheProductLeavesTheShop() throws Exception {
        send(post(URL), customerToken, Map.of("variantId", variant.getId())).andExpect(status().isOk());
        send(post(URL), otherToken, Map.of("variantId", second.getId())).andExpect(status().isOk());
        StoreFixtures.stock(entityManager, store, variant, 4);
        when(mailTransport.send(any())).thenReturn(false);

        JsonNode run = data(send(post("/api/v1/admin/stock-alerts/run"), adminToken, null).andExpect(status().isOk()));
        assertThat(run.get("backInStock").asInt()).isEqualTo(1);
        assertThat(run.get("mailed").asInt()).isZero();
        assertThat(ids(customerToken)).containsExactly(variant.getId());

        second.getProduct().setActive(false);
        productRepository.saveAndFlush(second.getProduct());
        send(post("/api/v1/admin/stock-alerts/run"), adminToken, null).andExpect(status().isOk());
        assertThat(ids(otherToken)).isEmpty();
        assertThat(ids(customerToken)).isEmpty();
    }

    private List<Long> ids(String token) throws Exception {
        List<Long> ids = new ArrayList<>();
        data(send(get(URL), token, null).andExpect(status().isOk())).forEach(node -> ids.add(node.asLong()));
        return ids;
    }

    private static ProductVariant variant(Product product, String name, String sku) {
        ProductVariant variant = new ProductVariant();
        variant.setProduct(product);
        variant.setVariantName(name);
        variant.setSkuVariant(sku);
        variant.setPrice(new BigDecimal("1000000"));
        return variant;
    }

    private JsonNode register(String username) throws Exception {
        JsonNode data = data(send(post("/api/v1/auth/register"), null, Map.of(
                "email", username + "@example.com", "username", username, "password", PASSWORD,
                "fullname", "Người Dùng Báo Hàng")).andExpect(status().isCreated()));
        registeredUserIds.add(data.get("user").get("id").asLong());
        return data;
    }

    private String registerAdmin(String username) throws Exception {
        Long id = register(username).get("user").get("id").asLong();
        userRoleRepository.deleteAll(userRoleRepository.findAllByIdUserId(id));
        userRoleRepository.save(new UserRole(userRepository.getReferenceById(id), roleRepository.findByName("ADMIN").orElseThrow()));
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
