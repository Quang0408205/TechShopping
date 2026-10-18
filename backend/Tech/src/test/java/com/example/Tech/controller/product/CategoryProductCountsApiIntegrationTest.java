package com.example.Tech.controller.product;

import com.example.Tech.entity.product.Category;
import com.example.Tech.entity.product.Product;
import jakarta.persistence.EntityManager;
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

/** GET /categories/product-counts (kế hoạch v6 B1): products on sale per category, public. */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class CategoryProductCountsApiIntegrationTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private EntityManager entityManager;

    @Test
    void countsOnlyProductsOnSale_publicly() throws Exception {
        Category category = new Category();
        category.setName("Danh mục đếm");
        category.setSlug("test-counts-category");
        entityManager.persist(category);
        product("test-count-1", category, true, false);
        product("test-count-2", category, true, false);
        product("test-count-hidden", category, false, false);
        product("test-count-deleted", category, true, true);
        entityManager.flush();

        mockMvc.perform(get("/api/v1/categories/product-counts"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[?(@.categoryId == " + category.getId() + ")].count").value(2));
    }

    private void product(String slug, Category category, boolean active, boolean deleted) {
        Product product = new Product();
        product.setName(slug);
        product.setSlug(slug);
        product.setCategory(category);
        product.setBasePrice(new BigDecimal("1000000"));
        product.setActive(active);
        if (deleted) {
            product.setDeletedAt(LocalDateTime.now());
        }
        entityManager.persist(product);
    }
}
