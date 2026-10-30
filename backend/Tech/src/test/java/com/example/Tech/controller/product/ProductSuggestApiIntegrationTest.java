package com.example.Tech.controller.product;

import com.example.Tech.entity.inventory.Inventory;
import com.example.Tech.entity.product.Brand;
import com.example.Tech.entity.product.Category;
import com.example.Tech.entity.product.Product;
import com.example.Tech.entity.product.ProductImage;
import com.example.Tech.entity.product.ProductVariant;
import com.example.Tech.entity.store.Store;
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

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** GET /products/suggest — public suggestions with image, price and stock, in-stock first. */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class ProductSuggestApiIntegrationTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private EntityManager entityManager;

    private Product pro;
    private Product plain;
    private Product electronic;

    @BeforeEach
    void setUp() {
        Category category = new Category();
        category.setName("Danh mục gợi ý thử");
        category.setSlug("test-sg-category");
        category.setActive(true);
        entityManager.persist(category);
        Brand kiwi = brand("Kiwizq", "test-sg-kiwizq");
        Brand other = brand("Hãng khác zq", "test-sg-other");
        Store store = new Store();
        store.setName("Chi nhánh gợi ý thử");
        store.setAddress("1 Đường thử");
        store.setCity("Hồ Chí Minh");
        store.setActive(true);
        entityManager.persist(store);

        pro = product("Zqsg Phone Pro", "zqsg-phone-pro", category, kiwi, true, "9000000", "8000000");
        plain = product("Zqsg Phone", "zqsg-phone", category, kiwi, true, "5000000", null);
        electronic = product("Máy Zqsg Điện Tử", "may-zqsg-dien-tu", category, other, true, "3000000", null);
        product("Zqsg Ẩn", "zqsg-an", category, kiwi, false, "1000000", null);
        stock(store, pro, 3);
        stock(store, electronic, 1);
        ProductImage image = new ProductImage();
        image.setProduct(pro);
        image.setImageUrl("https://img.example/zqsg-pro.jpg");
        image.setPrimary(true);
        image.setDisplayOrder(1);
        entityManager.persist(image);
        entityManager.flush();
    }

    @Test
    void inStockFirst_thenNamesStartingWithTheWord_withImagePriceAndTotal() throws Exception {
        mockMvc.perform(get("/api/v1/products/suggest").param("q", "ZQSG"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.total").value(3))
                .andExpect(jsonPath("$.data.items.length()").value(3))
                .andExpect(jsonPath("$.data.items[0].id").value(pro.getId()))
                .andExpect(jsonPath("$.data.items[0].imageUrl").value("https://img.example/zqsg-pro.jpg"))
                .andExpect(jsonPath("$.data.items[0].price").value(8000000))
                .andExpect(jsonPath("$.data.items[0].originalPrice").value(9000000))
                .andExpect(jsonPath("$.data.items[0].brandName").value("Kiwizq"))
                .andExpect(jsonPath("$.data.items[0].inStock").value(true))
                .andExpect(jsonPath("$.data.items[1].id").value(electronic.getId()))
                .andExpect(jsonPath("$.data.items[2].id").value(plain.getId()))
                .andExpect(jsonPath("$.data.items[2].inStock").value(false))
                .andExpect(jsonPath("$.data.items[2].originalPrice").doesNotExist());
    }

    @Test
    void everyWord_mayMatchTheBrand_orTheSlugWithoutAccents_andSizeLimits() throws Exception {
        mockMvc.perform(get("/api/v1/products/suggest").param("q", "kiwizq phone"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.total").value(2));
        mockMvc.perform(get("/api/v1/products/suggest").param("q", "dien tu zqsg"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.total").value(1))
                .andExpect(jsonPath("$.data.items[0].id").value(electronic.getId()));
        mockMvc.perform(get("/api/v1/products/suggest").param("q", "zqsg").param("size", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.total").value(3))
                .andExpect(jsonPath("$.data.items.length()").value(1));
    }

    @Test
    void blankQuery_orBadSize_isAValidationError() throws Exception {
        mockMvc.perform(get("/api/v1/products/suggest").param("q", "   "))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.error.details.q").exists());
        mockMvc.perform(get("/api/v1/products/suggest").param("q", "zqsg").param("size", "11"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.details.size").exists());
    }

    private Brand brand(String name, String slug) {
        Brand value = new Brand();
        value.setName(name);
        value.setSlug(slug);
        entityManager.persist(value);
        return value;
    }

    private Product product(String name, String slug, Category category, Brand brand, boolean active,
                            String price, String discount) {
        Product product = new Product();
        product.setName(name);
        product.setSlug(slug);
        product.setCategory(category);
        product.setBrand(brand);
        product.setBasePrice(new BigDecimal(price));
        product.setDiscountPrice(discount == null ? null : new BigDecimal(discount));
        product.setActive(active);
        entityManager.persist(product);
        return product;
    }

    private void stock(Store store, Product product, int quantity) {
        ProductVariant variant = new ProductVariant();
        variant.setProduct(product);
        variant.setVariantName("Mặc định");
        variant.setPrice(product.getBasePrice());
        entityManager.persist(variant);
        entityManager.persist(new Inventory(store, variant, quantity));
    }
}
