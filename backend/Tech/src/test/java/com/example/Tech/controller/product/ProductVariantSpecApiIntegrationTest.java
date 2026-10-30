package com.example.Tech.controller.product;

import com.example.Tech.entity.product.Brand;
import com.example.Tech.entity.product.Category;
import com.example.Tech.entity.user.User;
import com.example.Tech.repository.product.BrandRepository;
import com.example.Tech.repository.product.CategoryRepository;
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

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Images with a colour and specifications of one variant (rolled back, test database). */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class ProductVariantSpecApiIntegrationTest {

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

    private Integer categoryId;
    private Integer brandId;

    @BeforeEach
    void setUp() {
        Category category = new Category();
        category.setName("Phiên bản (test)");
        category.setSlug("test-variant-spec-api");
        categoryId = categoryRepository.save(category).getId();
        Brand brand = new Brand();
        brand.setName("Test Variant Spec Brand");
        brand.setSlug("test-variant-spec-brand");
        brandId = brandRepository.save(brand).getId();
    }

    @Test
    void imagesKeepTheirColour_andVariantSpecsAreTiedToTheirOwnVariant() throws Exception {
        String admin = token();
        Map<String, Object> body = new HashMap<>();
        body.put("name", "Máy hai màu");
        body.put("categoryId", categoryId);
        body.put("brandId", brandId);
        body.put("basePrice", 10000000);
        body.put("images", new ArrayList<>(List.of(
                image("https://cdn.example/main.jpg", true, null),
                image("https://cdn.example/den-1.jpg", false, "  Đen "),
                image("https://cdn.example/do-1.jpg", false, "Đỏ"))));
        long productId = data(send(post("/api/v1/products"), admin, body).andExpect(status().isCreated())).get("id").asLong();

        send(get("/api/v1/products/" + productId + "/images"), null, null)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(3))
                .andExpect(jsonPath("$.data[0].color").doesNotExist())
                .andExpect(jsonPath("$.data[1].color").value("Đen"))
                .andExpect(jsonPath("$.data[2].color").value("Đỏ"));

        long imageId = data(send(get("/api/v1/products/" + productId + "/images"), null, null)).get(1).get("id").asLong();
        Map<String, Object> update = new HashMap<>();
        update.put("imageUrl", "https://cdn.example/den-1.jpg");
        update.put("displayOrder", 1);
        update.put("color", "Xanh");
        send(put("/api/v1/product-images/" + imageId), admin, update)
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.color").value("Xanh"));

        long variantA = variant(admin, productId, "8GB/256GB - Đen", "8 GB", "256 GB", "Đen", 10000000);
        long variantB = variant(admin, productId, "12GB/512GB - Đen", "12 GB", "512 GB", "Đen", 14000000);

        spec(admin, productId, "Pin", "4000 mAh", null).andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.variantId").doesNotExist());
        spec(admin, productId, "Pin", "5000 mAh", variantB).andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.variantId").value(variantB));

        send(get("/api/v1/products/" + productId + "/specifications"), null, null)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(2));

        // a variant of another product is refused
        Map<String, Object> other = new HashMap<>(body);
        other.put("name", "Máy khác");
        long otherId = data(send(post("/api/v1/products"), admin, other).andExpect(status().isCreated())).get("id").asLong();
        spec(admin, otherId, "Pin", "1 mAh", variantA).andExpect(status().isBadRequest());

        // deleting the variant takes its own specification rows along; the common one stays
        send(delete("/api/v1/product-variants/" + variantB), admin, null).andExpect(status().isNoContent());
        send(get("/api/v1/products/" + productId + "/specifications"), null, null)
                .andExpect(jsonPath("$.data.length()").value(1))
                .andExpect(jsonPath("$.data[0].specValue").value("4000 mAh"));
    }

    private ResultActions spec(String token, long productId, String name, String value, Long variantId) throws Exception {
        Map<String, Object> body = new HashMap<>();
        body.put("productId", productId);
        body.put("specName", name);
        body.put("specValue", value);
        body.put("specOrder", 0);
        if (variantId != null) {
            body.put("variantId", variantId);
        }
        return send(post("/api/v1/product-specifications"), token, body);
    }

    private long variant(String token, long productId, String name, String ram, String storage, String color, int price) throws Exception {
        Map<String, Object> body = new HashMap<>();
        body.put("productId", productId);
        body.put("variantName", name);
        body.put("ram", ram);
        body.put("storage", storage);
        body.put("color", color);
        body.put("price", price);
        return data(send(post("/api/v1/product-variants"), token, body).andExpect(status().isCreated())).get("id").asLong();
    }

    private static Map<String, Object> image(String url, boolean primary, String color) {
        Map<String, Object> image = new HashMap<>();
        image.put("imageUrl", url);
        image.put("isPrimary", primary);
        if (color != null) {
            image.put("color", color);
        }
        return image;
    }

    private String token() {
        User user = new User();
        user.setId(123458L);
        user.setUsername("variant-spec-test");
        return "Bearer " + jwtTokenService.issueAccessToken(user, List.of("ADMIN"));
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
