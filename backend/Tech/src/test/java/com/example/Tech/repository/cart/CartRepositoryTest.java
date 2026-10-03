package com.example.Tech.repository.cart;

import com.example.Tech.entity.cart.Cart;
import com.example.Tech.entity.cart.CartItem;
import com.example.Tech.entity.product.Brand;
import com.example.Tech.entity.product.Category;
import com.example.Tech.entity.product.Product;
import com.example.Tech.entity.product.ProductVariant;
import com.example.Tech.entity.user.User;
import com.example.Tech.repository.product.BrandRepository;
import com.example.Tech.repository.product.CategoryRepository;
import com.example.Tech.repository.product.ProductRepository;
import com.example.Tech.repository.product.ProductVariantRepository;
import com.example.Tech.repository.user.UserRepository;
import jakarta.persistence.EntityManager;
import org.hibernate.Hibernate;
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
 * Checks the Cart group mapping and the native upserts against the real PostgreSQL test database
 * (techshopping_test). Every test is rolled back.
 */
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ActiveProfiles("test")
class CartRepositoryTest {

    private static final int MAX_LINE_QUANTITY = 10;

    @Autowired
    private CartRepository cartRepository;

    @Autowired
    private CartItemRepository cartItemRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired
    private BrandRepository brandRepository;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private ProductVariantRepository variantRepository;

    @Autowired
    private EntityManager entityManager;

    private User user;
    private ProductVariant black;
    private ProductVariant white;

    @BeforeEach
    void setUp() {
        User newUser = new User();
        newUser.setEmail("cart-test@techshopping.vn");
        newUser.setUsername("cart-test");
        newUser.setPasswordHash("{test}hash");
        newUser.setFullname("Cart Test");
        user = userRepository.save(newUser);

        Category category = new Category();
        category.setName("Điện thoại");
        category.setSlug("test-cart-dien-thoai");
        category = categoryRepository.save(category);

        Brand brand = new Brand();
        brand.setName("Test Cart Brand");
        brand.setSlug("test-cart-brand");
        brand = brandRepository.save(brand);

        Product product = new Product();
        product.setName("Điện thoại Test Giỏ Hàng");
        product.setSlug("test-cart-dien-thoai-test-gio-hang");
        product.setCategory(category);
        product.setBrand(brand);
        product.setBasePrice(new BigDecimal("15990000"));
        product = productRepository.save(product);

        black = variantRepository.save(variant(product, "Đen 128GB", "15990000"));
        white = variantRepository.save(variant(product, "Trắng 256GB", "18990000"));

        entityManager.flush();
        entityManager.clear();
    }

    @Test
    void insertIfMissing_createsOneCartPerUser() {
        assertThat(cartRepository.findByUserId(user.getId())).isEmpty();

        assertThat(cartRepository.insertIfMissing(user.getId())).isEqualTo(1);
        assertThat(cartRepository.insertIfMissing(user.getId())).isZero();

        Cart cart = cartRepository.findByUserId(user.getId()).orElseThrow();
        assertThat(cart.getCreatedAt()).isNotNull();
        assertThat(cart.getUpdatedAt()).isNotNull();
        assertThat(cart.getUser().getId()).isEqualTo(user.getId());
        assertThat(entityManager.createQuery("select count(c) from Cart c where c.user.id = :userId", Long.class)
                .setParameter("userId", user.getId())
                .getSingleResult()).isEqualTo(1L);
    }

    @Test
    void upsertQuantity_mergesTheLineAndCapsTheQuantity() {
        Long cartId = createCart();

        cartItemRepository.upsertQuantity(cartId, black.getId(), 3, MAX_LINE_QUANTITY);
        assertThat(quantityOf(cartId, black)).isEqualTo(3);

        cartItemRepository.upsertQuantity(cartId, black.getId(), 4, MAX_LINE_QUANTITY);
        assertThat(quantityOf(cartId, black)).isEqualTo(7);

        cartItemRepository.upsertQuantity(cartId, black.getId(), 9, MAX_LINE_QUANTITY);
        assertThat(quantityOf(cartId, black)).isEqualTo(MAX_LINE_QUANTITY);

        cartItemRepository.upsertQuantity(cartId, white.getId(), 1, MAX_LINE_QUANTITY);
        assertThat(cartItemRepository.countByCartId(cartId)).isEqualTo(2);
        assertThat(cartItemRepository.existsByCartIdAndVariantId(cartId, white.getId())).isTrue();
        assertThat(cartItemRepository.findByCartIdAndVariantId(cartId, white.getId()).orElseThrow().getAddedAt())
                .isNotNull();
    }

    @Test
    void findAllWithProduct_loadsVariantAndProductInAddedOrder() {
        Long cartId = createCart();
        Cart cart = cartRepository.getReferenceById(cartId);
        cartItemRepository.save(new CartItem(cart, variantRepository.getReferenceById(white.getId()), 2));
        cartItemRepository.save(new CartItem(cart, variantRepository.getReferenceById(black.getId()), 1));
        entityManager.flush();
        entityManager.clear();

        List<CartItem> items = cartItemRepository.findAllWithProductByCartId(cartId);

        assertThat(items).extracting(item -> item.getVariant().getId())
                .containsExactly(white.getId(), black.getId());
        CartItem first = items.getFirst();
        assertThat(Hibernate.isInitialized(first.getVariant())).isTrue();
        assertThat(Hibernate.isInitialized(first.getVariant().getProduct())).isTrue();
        assertThat(first.getVariant().getProduct().getName()).isEqualTo("Điện thoại Test Giỏ Hàng");
        assertThat(first.getQuantity()).isEqualTo(2);
        assertThat(cartItemRepository.findAllWithProductByCartId(-1L)).isEmpty();
    }

    @Test
    void updateAndDelete_affectOnlyTheTargetedLines() {
        Long cartId = createCart();
        cartItemRepository.upsertQuantity(cartId, black.getId(), 1, MAX_LINE_QUANTITY);
        cartItemRepository.upsertQuantity(cartId, white.getId(), 1, MAX_LINE_QUANTITY);

        assertThat(cartItemRepository.updateQuantity(cartId, black.getId(), 5)).isEqualTo(1);
        assertThat(quantityOf(cartId, black)).isEqualTo(5);
        assertThat(quantityOf(cartId, white)).isEqualTo(1);
        assertThat(cartItemRepository.updateQuantity(cartId, -1L, 5)).isZero();

        assertThat(cartItemRepository.deleteByCartIdAndVariantId(cartId, white.getId())).isEqualTo(1);
        assertThat(cartItemRepository.deleteByCartIdAndVariantId(cartId, white.getId())).isZero();
        assertThat(cartItemRepository.countByCartId(cartId)).isEqualTo(1);

        assertThat(cartItemRepository.deleteAllByCartId(cartId)).isEqualTo(1);
        assertThat(cartItemRepository.countByCartId(cartId)).isZero();
        assertThat(cartRepository.findById(cartId)).isPresent();
    }

    @Test
    void deleteAllByVariantId_letsTheVariantBeHardDeleted() {
        Long cartId = createCart();
        cartItemRepository.upsertQuantity(cartId, black.getId(), 2, MAX_LINE_QUANTITY);
        cartItemRepository.upsertQuantity(cartId, white.getId(), 1, MAX_LINE_QUANTITY);

        assertThat(cartItemRepository.deleteAllByVariantId(black.getId())).isEqualTo(1);
        variantRepository.deleteById(black.getId());
        entityManager.flush();

        assertThat(variantRepository.findById(black.getId())).isEmpty();
        assertThat(cartItemRepository.findAllWithProductByCartId(cartId))
                .extracting(item -> item.getVariant().getId())
                .containsExactly(white.getId());
    }

    @Test
    void touch_updatesTheCartTimestamp() {
        Long cartId = createCart();
        entityManager.createNativeQuery("update carts set updated_at = :old where cart_id = :cartId")
                .setParameter("old", LocalDateTime.of(2020, 1, 1, 0, 0))
                .setParameter("cartId", cartId)
                .executeUpdate();

        assertThat(cartRepository.touch(cartId)).isEqualTo(1);

        assertThat(cartRepository.findById(cartId).orElseThrow().getUpdatedAt())
                .isAfter(LocalDateTime.of(2020, 1, 2, 0, 0));
    }

    @Test
    void deletingTheUser_removesTheCartAndItsItems() {
        Long cartId = createCart();
        cartItemRepository.upsertQuantity(cartId, black.getId(), 1, MAX_LINE_QUANTITY);

        // Users are soft-deleted by the application; this checks the DB cascade users -> carts -> cart_items.
        entityManager.createNativeQuery("delete from users where user_id = :userId")
                .setParameter("userId", user.getId())
                .executeUpdate();
        entityManager.clear();

        assertThat(cartRepository.findById(cartId)).isEmpty();
        assertThat(cartItemRepository.countByCartId(cartId)).isZero();
    }

    private Long createCart() {
        cartRepository.insertIfMissing(user.getId());
        return cartRepository.findByUserId(user.getId()).orElseThrow().getId();
    }

    private int quantityOf(Long cartId, ProductVariant variant) {
        return cartItemRepository.findByCartIdAndVariantId(cartId, variant.getId()).orElseThrow().getQuantity();
    }

    private static ProductVariant variant(Product product, String name, String price) {
        ProductVariant variant = new ProductVariant();
        variant.setProduct(product);
        variant.setVariantName(name);
        variant.setPrice(new BigDecimal(price));
        return variant;
    }
}
