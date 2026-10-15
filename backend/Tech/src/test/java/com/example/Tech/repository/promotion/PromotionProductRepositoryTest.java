package com.example.Tech.repository.promotion;

import com.example.Tech.entity.product.Category;
import com.example.Tech.entity.product.Product;
import com.example.Tech.entity.promotion.DiscountType;
import com.example.Tech.entity.promotion.Promotion;
import com.example.Tech.entity.promotion.PromotionProduct;
import com.example.Tech.entity.user.User;
import com.example.Tech.repository.product.CategoryRepository;
import com.example.Tech.repository.product.ProductRepository;
import com.example.Tech.repository.user.UserRepository;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Checks the promotion overlap query and the active-promotion batch lookup against the real PostgreSQL
 * test database (techshopping_test). Every test is rolled back.
 */
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ActiveProfiles("test")
class PromotionProductRepositoryTest {

    @Autowired
    private PromotionRepository promotionRepository;

    @Autowired
    private PromotionProductRepository promotionProductRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private EntityManager entityManager;

    private User admin;
    private Product productA;
    private Product productB;

    @BeforeEach
    void setUp() {
        admin = userRepository.save(user("promo-test-admin"));

        Category category = new Category();
        category.setName("Điện thoại");
        category.setSlug("test-promo-dien-thoai");
        category = categoryRepository.save(category);

        productA = productRepository.save(product(category, "Điện thoại Test Khuyến Mãi A", "test-promo-a"));
        productB = productRepository.save(product(category, "Điện thoại Test Khuyến Mãi B", "test-promo-b"));

        entityManager.flush();
        entityManager.clear();
    }

    @Test
    void findOverlappingProductIds_detectsAnOverlappingActivePromotionOnTheSameProduct() {
        promote(productA, day(1), day(10), true, -1L);
        entityManager.flush();
        entityManager.clear();

        List<Long> overlapping = promotionProductRepository.findOverlappingProductIds(
                List.of(productA.getId(), productB.getId()), day(5), day(15), -1L);

        assertThat(overlapping).containsExactly(productA.getId());
    }

    @Test
    void findOverlappingProductIds_nonOverlappingWindow_returnsEmpty() {
        promote(productA, day(1), day(10), true, -1L);
        entityManager.flush();
        entityManager.clear();

        List<Long> overlapping = promotionProductRepository.findOverlappingProductIds(
                List.of(productA.getId()), day(11), day(20), -1L);

        assertThat(overlapping).isEmpty();
    }

    @Test
    void findOverlappingProductIds_touchingBoundary_isNotAnOverlap() {
        promote(productA, day(1), day(10), true, -1L);
        entityManager.flush();
        entityManager.clear();

        // new window starts exactly when the existing one ends: end == start, strict inequality excludes it
        List<Long> overlapping = promotionProductRepository.findOverlappingProductIds(
                List.of(productA.getId()), day(10), day(20), -1L);

        assertThat(overlapping).isEmpty();
    }

    @Test
    void findOverlappingProductIds_pausedPromotion_doesNotBlock() {
        promote(productA, day(1), day(10), false, -1L);
        entityManager.flush();
        entityManager.clear();

        List<Long> overlapping = promotionProductRepository.findOverlappingProductIds(
                List.of(productA.getId()), day(5), day(15), -1L);

        assertThat(overlapping).isEmpty();
    }

    @Test
    void findOverlappingProductIds_excludesItsOwnPromotionOnUpdate() {
        Promotion existing = promote(productA, day(1), day(10), true, -1L);
        entityManager.flush();
        entityManager.clear();

        List<Long> overlapping = promotionProductRepository.findOverlappingProductIds(
                List.of(productA.getId()), day(1), day(10), existing.getId());

        assertThat(overlapping).isEmpty();
    }

    @Test
    void findActiveForProducts_returnsOnlyThePromotionCoveringNow() {
        promote(productA, day(-5), day(5), true, -1L);
        promote(productB, day(10), day(20), true, -1L);
        entityManager.flush();
        entityManager.clear();

        List<PromotionProduct> active = promotionProductRepository.findActiveForProducts(
                List.of(productA.getId(), productB.getId()), day(0));

        assertThat(active).extracting(pp -> pp.getId().getProductId()).containsExactly(productA.getId());
    }

    private Promotion promote(Product product, LocalDateTime start, LocalDateTime end, boolean active, Long ignored) {
        Promotion promotion = new Promotion();
        promotion.setName("Khuyến mãi test");
        promotion.setDiscountType(DiscountType.PERCENTAGE);
        promotion.setDiscountValue(new BigDecimal("10"));
        promotion.setStartDate(start);
        promotion.setEndDate(end);
        promotion.setActive(active);
        promotion.setCreatedBy(userRepository.getReferenceById(admin.getId()));
        promotion = promotionRepository.save(promotion);

        PromotionProduct pp = new PromotionProduct(promotion, productRepository.getReferenceById(product.getId()),
                null, null);
        promotionProductRepository.save(pp);
        return promotion;
    }

    private static LocalDateTime day(int offset) {
        return LocalDateTime.of(2026, 1, 1, 0, 0).plusDays(offset);
    }

    private static User user(String username) {
        User user = new User();
        user.setEmail(username + "@techshopping.vn");
        user.setUsername(username);
        user.setPasswordHash("{test}hash");
        user.setFullname("Promotion Test Admin");
        return user;
    }

    private static Product product(Category category, String name, String slug) {
        Product product = new Product();
        product.setName(name);
        product.setSlug(slug);
        product.setCategory(category);
        product.setBasePrice(new BigDecimal("10000000"));
        return product;
    }
}
