package com.example.Tech.controller.recommendation;

import com.example.Tech.entity.product.Brand;
import com.example.Tech.entity.product.Category;
import com.example.Tech.entity.product.Need;
import com.example.Tech.entity.product.Product;
import com.example.Tech.entity.product.ProductNeed;
import com.example.Tech.entity.recommendation.InteractionType;
import com.example.Tech.entity.recommendation.UserInteraction;
import com.example.Tech.repository.product.BrandRepository;
import com.example.Tech.repository.product.CategoryRepository;
import com.example.Tech.repository.product.NeedRepository;
import com.example.Tech.repository.product.ProductNeedRepository;
import com.example.Tech.repository.product.ProductRepository;
import com.example.Tech.repository.recommendation.UserInteractionRepository;
import com.example.Tech.repository.recommendation.UserPreferenceRepository;
import com.example.Tech.security.RefreshTokenService;
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
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Shopping-preference questionnaire: public choices, a customer's answers (validation, replace, activity flag). */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class PreferenceApiIntegrationTest {

    private static final String PASSWORD = "Matkhau@123";

    @Autowired private MockMvc mockMvc;
    @Autowired private JsonMapper jsonMapper;
    @Autowired private RefreshTokenService refreshTokenService;
    @Autowired private CategoryRepository categoryRepository;
    @Autowired private BrandRepository brandRepository;
    @Autowired private NeedRepository needRepository;
    @Autowired private ProductRepository productRepository;
    @Autowired private ProductNeedRepository productNeedRepository;
    @Autowired private UserInteractionRepository interactionRepository;
    @Autowired private UserPreferenceRepository preferenceRepository;

    private final List<Long> registeredUserIds = new ArrayList<>();

    private String customer;
    private Long customerId;
    private Category phones;
    private Category laptops;
    private Brand brand;
    private Need photo;
    private Need office;
    private Product phone;

    @BeforeEach
    void setUp() throws Exception {
        JsonNode auth = register("pref.khach");
        customer = auth.get("accessToken").asString();
        customerId = auth.get("user").get("id").asLong();
        phones = category("Điện thoại Bảng Hỏi", "test-pref-phones", 1);
        laptops = category("Laptop Bảng Hỏi", "test-pref-laptops", 2);
        category("Danh mục trống Bảng Hỏi", "test-pref-empty", 3);    // no product on sale → not offered
        brand = new Brand();
        brand.setName("Hãng Bảng Hỏi");
        brand.setSlug("test-pref-brand");
        brand = brandRepository.save(brand);
        photo = need(phones, "test-pref-photo", "Chụp ảnh");
        office = need(laptops, "test-pref-office", "Văn phòng");
        need(phones, "test-pref-unused", "Chưa có sản phẩm");              // no product → not offered
        phone = product("Điện thoại Bảng Hỏi", "test-pref-phone", phones);
        product("Laptop Bảng Hỏi", "test-pref-laptop", laptops);
        ProductNeed productNeed = new ProductNeed();
        productNeed.setProductId(phone.getId());
        productNeed.setNeed(photo);
        productNeedRepository.save(productNeed);
    }

    @AfterEach
    void revokeTokens() {
        registeredUserIds.forEach(refreshTokenService::revokeAll);
    }

    @Test
    void options_arePublic_andListCategoriesOnSaleWithTheirBrandsAndNeeds() throws Exception {
        JsonNode options = data(send(get("/api/v1/recommendations/preference-options"), null, null)
                .andExpect(status().isOk()));

        JsonNode phoneOption = find(options, phones.getId());
        assertThat(phoneOption).isNotNull();
        assertThat(phoneOption.get("brands").get(0).get("id").asInt()).isEqualTo(brand.getId());
        assertThat(phoneOption.get("brands").get(0).get("productCount").asLong()).isEqualTo(1);
        assertThat(phoneOption.get("needs")).hasSize(1);
        assertThat(phoneOption.get("needs").get(0).get("id").asInt()).isEqualTo(photo.getId());
        assertThat(options.findValues("slug").stream().map(JsonNode::asString)).doesNotContain("test-pref-empty");
    }

    @Test
    void myAnswers_needAToken() throws Exception {
        send(get("/api/v1/users/me/preferences"), null, null).andExpect(status().isUnauthorized());
        send(put("/api/v1/users/me/preferences"), null, Map.of("categoryIds", List.of(phones.getId())))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void newCustomer_hasNotAnswered_thenSaves_andSavingAgainReplacesTheAnswers() throws Exception {
        JsonNode before = data(send(get("/api/v1/users/me/preferences"), customer, null).andExpect(status().isOk()));
        assertThat(before.get("answered").asBoolean()).isFalse();
        assertThat(before.get("hasActivity").asBoolean()).isFalse();

        JsonNode saved = data(send(put("/api/v1/users/me/preferences"), customer, Map.of(
                "categoryIds", List.of(phones.getId(), laptops.getId(), phones.getId()),
                "brandIds", List.of(brand.getId()),
                "priceLevels", List.of("MID", "HIGH"),
                "needIds", List.of(photo.getId(), office.getId())))
                .andExpect(status().isOk()));
        assertThat(saved.get("answered").asBoolean()).isTrue();
        assertThat(ids(saved.get("categoryIds"))).containsExactly(phones.getId(), laptops.getId());
        assertThat(saved.get("priceLevels").get(0).asString()).isEqualTo("MID");
        assertThat(saved.get("priceLevels").get(1).asString()).isEqualTo("HIGH");
        assertThat(preferenceRepository.findAllByUserIdOrderByIdAsc(customerId)).hasSize(7);

        data(send(put("/api/v1/users/me/preferences"), customer, Map.of("categoryIds", List.of(laptops.getId())))
                .andExpect(status().isOk()));
        JsonNode after = data(send(get("/api/v1/users/me/preferences"), customer, null).andExpect(status().isOk()));
        assertThat(ids(after.get("categoryIds"))).containsExactly(laptops.getId());
        assertThat(after.get("brandIds")).isEmpty();
        assertThat(after.get("needIds")).isEmpty();
    }

    @Test
    void answers_withoutCategory_withUnknownIds_orANeedOfAnotherCategory_areRejected() throws Exception {
        send(put("/api/v1/users/me/preferences"), customer, Map.of("categoryIds", List.of()))
                .andExpect(status().isBadRequest());
        send(put("/api/v1/users/me/preferences"), customer, Map.of("categoryIds", List.of(-5)))
                .andExpect(status().isBadRequest());
        send(put("/api/v1/users/me/preferences"), customer, Map.of(
                "categoryIds", List.of(phones.getId()), "brandIds", List.of(-7)))
                .andExpect(status().isBadRequest());
        send(put("/api/v1/users/me/preferences"), customer, Map.of(
                "categoryIds", List.of(phones.getId()), "priceLevels", List.of("CHEAP")))
                .andExpect(status().isBadRequest());
        // "Văn phòng" belongs to laptops, which was not chosen
        send(put("/api/v1/users/me/preferences"), customer, Map.of(
                "categoryIds", List.of(phones.getId()), "needIds", List.of(office.getId())))
                .andExpect(status().isBadRequest());

        assertThat(preferenceRepository.findAllByUserIdOrderByIdAsc(customerId)).isEmpty();
    }

    @Test
    void customerWithActivity_isReportedSoTheWebsiteDoesNotAsk() throws Exception {
        interactionRepository.save(new UserInteraction(customerId, phone.getId(), InteractionType.VIEW,
                LocalDateTime.now()));

        JsonNode answers = data(send(get("/api/v1/users/me/preferences"), customer, null).andExpect(status().isOk()));

        assertThat(answers.get("answered").asBoolean()).isFalse();
        assertThat(answers.get("hasActivity").asBoolean()).isTrue();
    }

    private Category category(String name, String slug, int order) {
        Category category = new Category();
        category.setName(name);
        category.setSlug(slug);
        category.setDisplayOrder(order);
        return categoryRepository.save(category);
    }

    private Need need(Category category, String slug, String name) {
        Need need = new Need();
        need.setCategoryId(category.getId());
        need.setSlug(slug);
        need.setName(name);
        return needRepository.save(need);
    }

    private Product product(String name, String slug, Category category) {
        Product product = new Product();
        product.setName(name);
        product.setSlug(slug);
        product.setCategory(category);
        product.setBrand(brand);
        product.setBasePrice(new BigDecimal("10000000"));
        return productRepository.save(product);
    }

    private static JsonNode find(JsonNode options, Integer categoryId) {
        for (JsonNode option : options) {
            if (option.get("id").asInt() == categoryId) {
                return option;
            }
        }
        return null;
    }

    private static List<Integer> ids(JsonNode array) {
        List<Integer> ids = new ArrayList<>();
        array.forEach(node -> ids.add(node.asInt()));
        return ids;
    }

    private JsonNode register(String username) throws Exception {
        JsonNode data = data(send(post("/api/v1/auth/register"), null, Map.of(
                "email", username + "@example.com", "username", username, "password", PASSWORD,
                "fullname", "Người Thử Bảng Hỏi"))
                .andExpect(status().isCreated()));
        registeredUserIds.add(data.get("user").get("id").asLong());
        return data;
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
