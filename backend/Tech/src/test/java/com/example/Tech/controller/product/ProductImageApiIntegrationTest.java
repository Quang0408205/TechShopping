package com.example.Tech.controller.product;

import com.example.Tech.entity.product.Brand;
import com.example.Tech.entity.product.Category;
import com.example.Tech.entity.user.User;
import com.example.Tech.repository.product.BrandRepository;
import com.example.Tech.repository.product.CategoryRepository;
import com.example.Tech.repository.product.ProductRepository;
import com.example.Tech.security.JwtTokenService;
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
import org.springframework.test.web.servlet.request.MockMultipartHttpServletRequestBuilder;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.endsWith;
import static org.hamcrest.Matchers.startsWith;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Product images over HTTP against the test database (rolled back): creating a product with its images,
 * primaryImageUrl in product responses, image rules, and the upload endpoint (files go to a temp folder,
 * see application-test.yml).
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class ProductImageApiIntegrationTest {

    private static final String UPLOAD_URL = "/api/v1/admin/uploads/product-images";
    private static final String UPLOADED_PREFIX = "http://localhost:8080/uploads/products/";
    private static final byte[] PNG = Arrays.copyOf(
            new byte[]{(byte) 0x89, 'P', 'N', 'G', '\r', '\n', 0x1A, '\n'}, 64);

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
    private ProductRepository productRepository;

    private Integer categoryId;
    private Integer brandId;

    @BeforeEach
    void setUp() {
        Category category = new Category();
        category.setName("Ảnh sản phẩm (test)");
        category.setSlug("test-product-image-api");
        categoryId = categoryRepository.save(category).getId();
        Brand brand = new Brand();
        brand.setName("Test Image API Brand");
        brand.setSlug("test-image-api-brand");
        brandId = brandRepository.save(brand).getId();
    }

    @Test
    void createProduct_withPrimaryAndSecondaryImages_savesThemInListOrder() throws Exception {
        JsonNode created = data(send(post("/api/v1/products"), token("ADMIN"), productBody("Laptop Ảnh A",
                image("https://cdn.example/a-side.jpg", false), image(" https://cdn.example/a-main.jpg ", true),
                image("https://cdn.example/a-back.jpg", null)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.primaryImageUrl").value("https://cdn.example/a-main.jpg")));
        long productId = created.get("id").asLong();

        send(get("/api/v1/products/" + productId + "/images"), null, null)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(3))
                .andExpect(jsonPath("$.data[0].imageUrl").value("https://cdn.example/a-side.jpg"))
                .andExpect(jsonPath("$.data[0].displayOrder").value(0))
                .andExpect(jsonPath("$.data[0].isPrimary").value(false))
                .andExpect(jsonPath("$.data[1].isPrimary").value(true))
                .andExpect(jsonPath("$.data[2].displayOrder").value(2));

        send(get("/api/v1/products/" + productId), null, null)
                .andExpect(jsonPath("$.data.primaryImageUrl").value("https://cdn.example/a-main.jpg"));
        send(get("/api/v1/products").param("categoryId", String.valueOf(categoryId)), null, null)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content[0].primaryImageUrl").value("https://cdn.example/a-main.jpg"));
    }

    @Test
    void createProduct_withoutEnoughValidImages_isRejectedAndCreatesNothing() throws Exception {
        long before = productRepository.count();

        send(post("/api/v1/products"), token("ADMIN"), productBody("Laptop Ảnh B"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.error.details.images").exists());
        Map<String, Object> withoutImages = productBody("Laptop Ảnh B");
        withoutImages.remove("images");
        send(post("/api/v1/products"), token("ADMIN"), withoutImages)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.details.images").value("Images are required"));
        send(post("/api/v1/products"), token("ADMIN"), productBody("Laptop Ảnh B",
                image("https://cdn.example/only.jpg", true)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.details.images").exists());
        send(post("/api/v1/products"), token("ADMIN"), productBody("Laptop Ảnh B",
                image("https://cdn.example/b.jpg", true), image("javascript:alert(1)", false)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.details['images[1].imageUrl']")
                        .value("Image URL must start with http:// or https://"));
        send(post("/api/v1/products"), token("ADMIN"), productBody("Laptop Ảnh B",
                image("https://cdn.example/b1.jpg", true), image("https://cdn.example/b2.jpg", true)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("INVALID_PRODUCT_DATA"));
        send(post("/api/v1/products"), token("ADMIN"), productBody("Laptop Ảnh B",
                image("https://cdn.example/b1.jpg", false), image("https://cdn.example/b2.jpg", false)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("INVALID_PRODUCT_DATA"));

        assertThat(productRepository.count()).isEqualTo(before);
    }

    @Test
    void imageRules_lastImageStays_primaryCannotBeUnset_deletingThePrimaryPromotesTheNext() throws Exception {
        long productId = data(send(post("/api/v1/products"), token("ADMIN"), productBody("Laptop Ảnh C",
                image("https://cdn.example/c-main.jpg", true), image("https://cdn.example/c-side.jpg", false)))
                .andExpect(status().isCreated())).get("id").asLong();
        JsonNode images = data(send(get("/api/v1/products/" + productId + "/images"), null, null));
        long mainId = images.get(0).get("id").asLong();
        long sideId = images.get(1).get("id").asLong();

        send(put("/api/v1/product-images/" + mainId), token("ADMIN"),
                Map.of("imageUrl", "https://cdn.example/c-main.jpg", "isPrimary", false))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error.code").value("PRIMARY_IMAGE_REQUIRED"));
        send(post("/api/v1/product-images"), token("ADMIN"),
                Map.of("productId", productId, "imageUrl", "ftp://cdn.example/x.jpg"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.details.imageUrl").exists());

        send(delete("/api/v1/product-images/" + mainId), token("ADMIN"), null)
                .andExpect(status().isNoContent());
        send(get("/api/v1/products/" + productId + "/images"), null, null)
                .andExpect(jsonPath("$.data.length()").value(1))
                .andExpect(jsonPath("$.data[0].id").value(sideId))
                .andExpect(jsonPath("$.data[0].isPrimary").value(true));
        send(get("/api/v1/products/" + productId), null, null)
                .andExpect(jsonPath("$.data.primaryImageUrl").value("https://cdn.example/c-side.jpg"));

        send(delete("/api/v1/product-images/" + sideId), token("ADMIN"), null)
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error.code").value("LAST_PRODUCT_IMAGE"));
    }

    @Test
    void upload_requiresAdmin() throws Exception {
        mockMvc.perform(upload(new MockMultipartFile("file", "a.png", "image/png", PNG), null))
                .andExpect(status().isUnauthorized());
        for (String role : List.of("CUSTOMER", "STAFF")) {
            mockMvc.perform(upload(new MockMultipartFile("file", "a.png", "image/png", PNG), token(role)))
                    .andExpect(status().isForbidden())
                    .andExpect(jsonPath("$.error.code").value("ACCESS_DENIED"));
        }
    }

    @Test
    void upload_png_returnsAPublicUrlThatServesTheFileWithoutLogin() throws Exception {
        String url = data(mockMvc.perform(upload(
                        new MockMultipartFile("file", "../anh san pham.png", "image/png", PNG), token("ADMIN")))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.url", startsWith(UPLOADED_PREFIX)))
                .andExpect(jsonPath("$.data.url", endsWith(".png")))).get("url").asString();

        mockMvc.perform(get(url.substring("http://localhost:8080".length())))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.IMAGE_PNG))
                .andExpect(content().bytes(PNG))
                .andExpect(header().string(HttpHeaders.CACHE_CONTROL, containsString("max-age")));

        // the uploaded URL is accepted as a product image
        send(post("/api/v1/products"), token("ADMIN"), productBody("Laptop Ảnh D",
                image(url, true), image("https://cdn.example/d-side.jpg", false)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.primaryImageUrl").value(url));
    }

    @Test
    void upload_rejectsNonImagesMissingFilesAndJsonBodies() throws Exception {
        mockMvc.perform(upload(new MockMultipartFile("file", "x.png", "image/png",
                        "<svg onload=alert(1)>".getBytes()), token("ADMIN")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("INVALID_IMAGE_FILE"));
        mockMvc.perform(upload(new MockMultipartFile("other", "x.png", "image/png", PNG), token("ADMIN")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.error.details.file").exists());
        mockMvc.perform(post(UPLOAD_URL).header(HttpHeaders.AUTHORIZATION, token("ADMIN"))
                        .contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isUnsupportedMediaType());
    }

    @Test
    void uploads_unknownFile_is404() throws Exception {
        mockMvc.perform(get("/uploads/products/00000000-0000-0000-0000-000000000000.png"))
                .andExpect(status().isNotFound());
    }

    private String token(String... roles) {
        User user = new User();
        user.setId(123457L);
        user.setUsername("image-test");
        return "Bearer " + jwtTokenService.issueAccessToken(user, List.of(roles));
    }

    private MockMultipartHttpServletRequestBuilder upload(MockMultipartFile file, String token) {
        MockMultipartHttpServletRequestBuilder builder = multipart(UPLOAD_URL).file(file);
        if (token != null) {
            builder.header(HttpHeaders.AUTHORIZATION, token);
        }
        return builder;
    }

    private Map<String, Object> productBody(String name, Map<?, ?>... images) {
        Map<String, Object> body = new HashMap<>();
        body.put("name", name);
        body.put("categoryId", categoryId);
        body.put("brandId", brandId);
        body.put("basePrice", 15990000);
        body.put("images", new ArrayList<>(List.of(images)));
        return body;
    }

    private static Map<String, Object> image(String url, Boolean primary) {
        Map<String, Object> image = new HashMap<>();
        image.put("imageUrl", url);
        image.put("isPrimary", primary);
        return image;
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
