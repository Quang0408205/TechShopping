package com.example.Tech.controller.product;

import com.example.Tech.entity.product.Brand;
import com.example.Tech.entity.product.Category;
import com.example.Tech.entity.product.Product;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** GET /categories/{id}/brands: the brands of the products on sale in the category (public). */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class CategoryBrandsApiIntegrationTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private EntityManager entityManager;

    private Category category;
    private Brand zeta;
    private Brand alpha;
    private Brand unused;

    @BeforeEach
    void setUp() {
        category = category("Danh mục hãng thử", "test-cb-category", true);
        zeta = brand("Zeta Thử");
        alpha = brand("Alpha Thử");
        unused = brand("Không dùng Thử");
        product("test-cb-1", category, zeta, true, false);
        product("test-cb-2", category, zeta, true, false);
        product("test-cb-3", category, alpha, true, false);
        product("test-cb-hidden", category, unused, false, false);
        product("test-cb-deleted", category, unused, true, true);
        product("test-cb-other", category("Danh mục khác", "test-cb-other-cat", true), unused, true, false);
        entityManager.flush();
    }

    @Test
    void returnsTheBrandsOfProductsOnSale_byName_withoutDuplicates() throws Exception {
        mockMvc.perform(get("/api/v1/categories/" + category.getId() + "/brands"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(2))
                .andExpect(jsonPath("$.data[0].id").value(alpha.getId()))
                .andExpect(jsonPath("$.data[0].name").value("Alpha Thử"))
                .andExpect(jsonPath("$.data[1].id").value(zeta.getId()));
    }

    @Test
    void hiddenCategory_hasNoBrands() throws Exception {
        category.setActive(false);
        entityManager.flush();

        mockMvc.perform(get("/api/v1/categories/" + category.getId() + "/brands"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(0));
    }

    @Test
    void unknownCategory_is404() throws Exception {
        mockMvc.perform(get("/api/v1/categories/999999/brands"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error.code").value("CATEGORY_NOT_FOUND"));
    }

    private Category category(String name, String slug, boolean active) {
        Category value = new Category();
        value.setName(name);
        value.setSlug(slug);
        value.setActive(active);
        entityManager.persist(value);
        return value;
    }

    private Brand brand(String name) {
        Brand value = new Brand();
        value.setName(name);
        value.setSlug("test-cb-" + name.toLowerCase().replace(' ', '-').replace("ử", "u"));
        entityManager.persist(value);
        return value;
    }

    private void product(String slug, Category category, Brand brand, boolean active, boolean deleted) {
        Product product = new Product();
        product.setName(slug);
        product.setSlug(slug);
        product.setCategory(category);
        product.setBrand(brand);
        product.setBasePrice(new BigDecimal("1000000"));
        product.setActive(active);
        if (deleted) {
            product.setDeletedAt(LocalDateTime.now());
        }
        entityManager.persist(product);
    }
}
