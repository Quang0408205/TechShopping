package com.example.Tech.controller.product;

import com.example.Tech.entity.product.Brand;
import com.example.Tech.entity.product.Category;
import com.example.Tech.entity.product.Need;
import com.example.Tech.entity.user.User;
import com.example.Tech.repository.product.BrandRepository;
import com.example.Tech.repository.product.CategoryRepository;
import com.example.Tech.repository.product.NeedRepository;
import com.example.Tech.security.JwtTokenService;
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

/** Normalised spec filters, needs and the product list filters (rolled back, test database). */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class ProductFilterApiIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JsonMapper jsonMapper;

    @Autowired
    private JwtTokenService jwtTokenService;

    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired
    private BrandRepository brandRepository;

    @Autowired
    private NeedRepository needRepository;

    private Integer categoryId;
    private Integer brandId;
    private Integer gamingNeedId;
    private Integer otherCategoryNeedId;

    @BeforeEach
    void setUp() {
        Category category = new Category();
        category.setName("Điện thoại (lọc test)");
        category.setSlug("test-dien-thoai-filter");
        categoryId = categoryRepository.save(category).getId();
        Category other = new Category();
        other.setName("Laptop (lọc test)");
        other.setSlug("test-laptop-filter");
        Integer otherId = categoryRepository.save(other).getId();
        Brand brand = new Brand();
        brand.setName("Test Filter Brand");
        brand.setSlug("test-filter-brand");
        brandId = brandRepository.save(brand).getId();
        gamingNeedId = need(categoryId, "choi-game", "Chơi game", 0);
        otherCategoryNeedId = need(otherId, "gaming", "Gaming", 0);
    }

    private Integer need(Integer category, String slug, String name, int order) {
        Need need = new Need();
        need.setCategoryId(category);
        need.setSlug(slug);
        need.setName(name);
        need.setDisplayOrder(order);
        return needRepository.save(need).getId();
    }

    @Test
    void filtersAreBuiltFromSpecsAndVariants_andNarrowTheProductList() throws Exception {
        String admin = token("ADMIN");
        long a = product(admin, "Máy A", "8 GB", "128 GB", "5000 mAh", "Snapdragon 8 Elite");
        long b = product(admin, "Máy B", "12 GB", "256 GB", "4500 mAh", "Apple A18");

        send(get("/api/v1/categories/" + categoryId + "/filters"), null, null)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[?(@.key=='ram')].values[*].value").value(org.hamcrest.Matchers.hasItems("8GB", "12GB")))
                .andExpect(jsonPath("$.data[?(@.key=='chip')].values.length()").value(2))
                .andExpect(jsonPath("$.data[?(@.key=='battery')].values[?(@.value=='5000 – 5999 mAh')].count").value(1))
                .andExpect(jsonPath("$.data[?(@.key=='cpu')]").isEmpty());

        // same group = OR
        assertThat(ids(search("filter=ram:8GB&filter=ram:12GB"))).containsExactlyInAnyOrder(a, b);
        // different groups = AND
        assertThat(ids(search("filter=ram:12GB&filter=chip:Apple A-series"))).containsExactly(b);
        assertThat(ids(search("filter=ram:8GB&filter=chip:Apple A-series"))).isEmpty();
        assertThat(ids(search("filter=ram:99GB"))).isEmpty();

        // a spec change rebuilds the index at once
        long specId = data(send(get("/api/v1/products/" + a + "/specifications"), null, null)).get(0).get("id").asLong();
        Map<String, Object> change = new HashMap<>();
        change.put("specName", "Chip xử lý (CPU)");
        change.put("specValue", "Apple A19 Pro");
        send(put("/api/v1/product-specifications/" + specId), admin, change).andExpect(status().isOk());
        assertThat(ids(search("filter=chip:Apple A-series"))).containsExactlyInAnyOrder(a, b);
    }

    @Test
    void needs_areLinkedToProducts_filterTheList_andMustMatchTheCategory() throws Exception {
        String admin = token("ADMIN");
        long a = product(admin, "Máy chơi game", "8 GB", "128 GB", "5000 mAh", "Snapdragon 8 Elite");
        long b = product(admin, "Máy văn phòng", "4 GB", "64 GB", "4000 mAh", "Unisoc T107");

        send(put("/api/v1/products/" + a + "/needs"), admin, Map.of("needIds", List.of(gamingNeedId)))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data[0].slug").value("choi-game"));
        send(get("/api/v1/products/" + a + "/needs"), null, null).andExpect(jsonPath("$.data.length()").value(1));
        send(get("/api/v1/products/" + b + "/needs"), null, null).andExpect(jsonPath("$.data.length()").value(0));

        assertThat(ids(search("need=choi-game"))).containsExactly(a);
        send(get("/api/v1/categories/" + categoryId + "/filters"), null, null)
                .andExpect(jsonPath("$.data[0].key").value("need"))
                .andExpect(jsonPath("$.data[0].values[0].value").value("choi-game"))
                .andExpect(jsonPath("$.data[0].values[0].count").value(1));
        send(get("/api/v1/categories/" + categoryId + "/needs"), null, null)
                .andExpect(jsonPath("$.data[0].productCount").value(1));

        // a need of another category is refused; customers cannot change needs; the list can be emptied
        send(put("/api/v1/products/" + a + "/needs"), admin, Map.of("needIds", List.of(otherCategoryNeedId)))
                .andExpect(status().isBadRequest());
        send(put("/api/v1/products/" + a + "/needs"), token("CUSTOMER"), Map.of("needIds", List.of())).andExpect(status().isForbidden());
        send(put("/api/v1/products/" + a + "/needs"), admin, Map.of("needIds", List.of())).andExpect(status().isOk());
        assertThat(ids(search("need=choi-game"))).isEmpty();
    }

    @Test
    void reindexAll_isAdminOnly() throws Exception {
        send(post("/api/v1/admin/products/reindex-filters"), token("CUSTOMER"), null).andExpect(status().isForbidden());
        send(post("/api/v1/admin/products/reindex-filters"), null, null).andExpect(status().isUnauthorized());
        send(post("/api/v1/admin/products/reindex-filters"), token("ADMIN"), null)
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.products").isNumber());
    }

    private ResultActions search(String query) throws Exception {
        return send(get("/api/v1/products?isActive=true&size=50&categoryId=" + categoryId + "&" + query), null, null).andExpect(status().isOk());
    }

    private List<Long> ids(ResultActions result) throws Exception {
        List<Long> list = new ArrayList<>();
        for (JsonNode node : data(result).get("content")) {
            list.add(node.get("id").asLong());
        }
        return list;
    }

    private long product(String admin, String name, String ram, String storage, String battery, String chip) throws Exception {
        Map<String, Object> body = new HashMap<>();
        body.put("name", name);
        body.put("categoryId", categoryId);
        body.put("brandId", brandId);
        body.put("basePrice", 10000000);
        body.put("images", new ArrayList<>(List.of(
                Map.of("imageUrl", "https://cdn.example/" + name.hashCode() + "-1.jpg", "isPrimary", true),
                Map.of("imageUrl", "https://cdn.example/" + name.hashCode() + "-2.jpg", "isPrimary", false))));
        long id = data(send(post("/api/v1/products"), admin, body).andExpect(status().isCreated())).get("id").asLong();
        Map<String, Object> variant = new HashMap<>();
        variant.put("productId", id);
        variant.put("variantName", ram + "/" + storage);
        variant.put("ram", ram);
        variant.put("storage", storage);
        variant.put("price", 10000000);
        send(post("/api/v1/product-variants"), admin, variant).andExpect(status().isCreated());
        spec(admin, id, "Chip xử lý (CPU)", chip, 0);
        spec(admin, id, "Dung lượng pin", battery, 1);
        return id;
    }

    private void spec(String admin, long productId, String name, String value, int order) throws Exception {
        Map<String, Object> body = new HashMap<>();
        body.put("productId", productId);
        body.put("specName", name);
        body.put("specValue", value);
        body.put("specOrder", order);
        send(post("/api/v1/product-specifications"), admin, body).andExpect(status().isCreated());
    }

    private String token(String role) {
        User user = new User();
        user.setId(123459L);
        user.setUsername("filter-test");
        return "Bearer " + jwtTokenService.issueAccessToken(user, List.of(role));
    }

    private ResultActions send(MockHttpServletRequestBuilder builder, String token, Object body) throws Exception {
        if (token != null) {
            builder.header(HttpHeaders.AUTHORIZATION, token);
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
