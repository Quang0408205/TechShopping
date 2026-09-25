package com.example.Tech.repository.product;

import com.example.Tech.dto.request.product.ProductSearchRequest;
import com.example.Tech.entity.product.Brand;
import com.example.Tech.entity.product.Category;
import com.example.Tech.entity.product.Product;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Runs the product filters against the real PostgreSQL test database (techshopping_test).
 * Every test runs in a transaction that is rolled back, so the database stays unchanged.
 */
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ActiveProfiles("test")
class ProductFilterSpecificationsTest {

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired
    private BrandRepository brandRepository;

    private Category phones;
    private Category laptops;
    private Brand apple;
    private Brand samsung;

    @BeforeEach
    void setUp() {
        phones = categoryRepository.save(category("Điện thoại", "test-dien-thoai"));
        laptops = categoryRepository.save(category("Laptop", "test-laptop"));
        apple = brandRepository.save(brand("Test Apple", "test-apple"));
        samsung = brandRepository.save(brand("Test Samsung", "test-samsung"));

        productRepository.save(product("Điện thoại iPhone 18 Pro 256GB", "test-dien-thoai-iphone-18-pro-256gb",
                phones, apple, "38990000", true, false));
        productRepository.save(product("Điện thoại Samsung Galaxy S26 5G", "test-dien-thoai-samsung-galaxy-s26-5g",
                phones, samsung, "19500000", true, false));
        productRepository.save(product("Laptop MacBook Air 13 inch M5", "test-laptop-macbook-air-13-inch-m5",
                laptops, apple, "35290000", false, false));
        productRepository.save(product("Điện thoại đã xoá 100% sale", "test-dien-thoai-da-xoa-100-sale",
                phones, samsung, "1000000", true, true));
    }

    @Test
    void noFilter_excludesSoftDeleted() {
        assertThat(search(filter(null, null, null, null, null, null))).hasSize(3);
    }

    @Test
    void keyword_matchesNameCaseInsensitive() {
        assertThat(search(filter("IPHONE", null, null, null, null, null)))
                .extracting(Product::getName)
                .containsExactly("Điện thoại iPhone 18 Pro 256GB");
    }

    @Test
    void keyword_withoutAccents_matchesSlug() {
        assertThat(search(filter("dien thoai galaxy", null, null, null, null, null)))
                .extracting(Product::getName)
                .containsExactly("Điện thoại Samsung Galaxy S26 5G");
    }

    @Test
    void keyword_wordsMatchInAnyOrder() {
        assertThat(search(filter("256gb iphone", null, null, null, null, null)))
                .extracting(Product::getName)
                .containsExactly("Điện thoại iPhone 18 Pro 256GB");
        assertThat(search(filter("iphone galaxy", null, null, null, null, null))).isEmpty();
    }

    @Test
    void keyword_likeWildcardsAreEscaped() {
        // "%" must be treated literally; the only match is soft-deleted
        assertThat(search(filter("100%", null, null, null, null, null))).isEmpty();
        assertThat(search(filter("_", null, null, null, null, null))).isEmpty();
    }

    @Test
    void categoryAndBrand_filtersCombineWithAnd() {
        assertThat(search(filter(null, phones.getId(), null, null, null, null))).hasSize(2);
        assertThat(search(filter(null, null, apple.getId(), null, null, null))).hasSize(2);
        assertThat(search(filter(null, phones.getId(), apple.getId(), null, null, null)))
                .extracting(Product::getName)
                .containsExactly("Điện thoại iPhone 18 Pro 256GB");
    }

    @Test
    void isActive_filter() {
        assertThat(search(filter(null, null, null, false, null, null)))
                .extracting(Product::getName)
                .containsExactly("Laptop MacBook Air 13 inch M5");
    }

    @Test
    void priceRange_isInclusive() {
        assertThat(search(filter(null, null, null, null, new BigDecimal("19500000"), new BigDecimal("35290000"))))
                .extracting(Product::getName)
                .containsExactlyInAnyOrder("Điện thoại Samsung Galaxy S26 5G", "Laptop MacBook Air 13 inch M5");
    }

    @Test
    void pagination_andSorting() {
        Page<Product> firstPage = productRepository.findAll(
                ProductFilterSpecifications.matching(filter(null, null, null, null, null, null)),
                PageRequest.of(0, 2, Sort.by(Sort.Direction.DESC, "basePrice")));

        assertThat(firstPage.getTotalElements()).isEqualTo(3);
        assertThat(firstPage.getTotalPages()).isEqualTo(2);
        assertThat(firstPage.getContent()).extracting(Product::getBasePrice)
                .usingElementComparator(BigDecimal::compareTo)
                .containsExactly(new BigDecimal("38990000"), new BigDecimal("35290000"));
        // category and brand are fetched with the page (entity graph)
        assertThat(firstPage.getContent().getFirst().getCategory().getName()).isEqualTo("Điện thoại");
    }

    private java.util.List<Product> search(ProductSearchRequest filter) {
        return productRepository.findAll(ProductFilterSpecifications.matching(filter),
                PageRequest.of(0, 50, Sort.by("id"))).getContent();
    }

    private static ProductSearchRequest filter(String keyword, Integer categoryId, Integer brandId, Boolean isActive,
                                               BigDecimal minPrice, BigDecimal maxPrice) {
        return new ProductSearchRequest(keyword, categoryId, brandId, isActive, minPrice, maxPrice);
    }

    private static Category category(String name, String slug) {
        Category category = new Category();
        category.setName(name);
        category.setSlug(slug);
        return category;
    }

    private static Brand brand(String name, String slug) {
        Brand brand = new Brand();
        brand.setName(name);
        brand.setSlug(slug);
        return brand;
    }

    private static Product product(String name, String slug, Category category, Brand brand, String price,
                                   boolean active, boolean deleted) {
        Product product = new Product();
        product.setName(name);
        product.setSlug(slug);
        product.setCategory(category);
        product.setBrand(brand);
        product.setBasePrice(new BigDecimal(price));
        product.setActive(active);
        if (deleted) {
            product.setDeletedAt(LocalDateTime.now());
        }
        return product;
    }
}
