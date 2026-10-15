package com.example.Tech.repository.inventory;

import com.example.Tech.entity.inventory.Inventory;
import com.example.Tech.entity.inventory.MovementType;
import com.example.Tech.entity.inventory.StockMovement;
import com.example.Tech.entity.product.Category;
import com.example.Tech.entity.product.Product;
import com.example.Tech.entity.product.ProductVariant;
import com.example.Tech.entity.store.Store;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.ActiveProfiles;

import com.example.Tech.repository.product.CategoryRepository;
import com.example.Tech.repository.product.ProductRepository;
import com.example.Tech.repository.product.ProductVariantRepository;
import com.example.Tech.repository.store.StoreRepository;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Checks the Inventory/StockMovement mapping, the composite key, and the lock/absent-row semantics
 * against the real PostgreSQL test database (techshopping_test). Every test is rolled back.
 */
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ActiveProfiles("test")
class InventoryRepositoryTest {

    @Autowired
    private InventoryRepository inventoryRepository;

    @Autowired
    private StockMovementRepository stockMovementRepository;

    @Autowired
    private StoreRepository storeRepository;

    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private ProductVariantRepository variantRepository;

    @Autowired
    private EntityManager entityManager;

    private Store store;
    private ProductVariant variantA;
    private ProductVariant variantB;

    @BeforeEach
    void setUp() {
        store = storeRepository.save(store("InventoryRepoTest Store"));

        Category category = new Category();
        category.setName("Điện thoại");
        category.setSlug("test-inventory-dien-thoai");
        category = categoryRepository.save(category);

        Product product = new Product();
        product.setName("Điện thoại Test Tồn Kho");
        product.setSlug("test-inventory-dien-thoai-test-ton-kho");
        product.setCategory(category);
        product.setBasePrice(new BigDecimal("10000000"));
        product = productRepository.save(product);

        variantA = variantRepository.save(variant(product, "Đen"));
        variantB = variantRepository.save(variant(product, "Trắng"));
        entityManager.flush();
        entityManager.clear();
    }

    @Test
    void absentRow_isNotFoundAndNotLocked() {
        assertThat(inventoryRepository.findByIdStoreIdAndIdVariantId(store.getId(), variantA.getId())).isEmpty();
        assertThat(inventoryRepository.findAllForUpdate(store.getId(), List.of(variantA.getId(), variantB.getId())))
                .isEmpty();
    }

    @Test
    void save_setsCompositeKeyAndUpdatedAt() {
        Inventory inventory = new Inventory(storeRepository.getReferenceById(store.getId()),
                variantRepository.getReferenceById(variantA.getId()), 5);
        inventoryRepository.save(inventory);
        entityManager.flush();
        entityManager.clear();

        Inventory found = inventoryRepository.findByIdStoreIdAndIdVariantId(store.getId(), variantA.getId())
                .orElseThrow();
        assertThat(found.getQuantity()).isEqualTo(5);
        assertThat(found.getUpdatedAt()).isNotNull();
        assertThat(found.getId().getStoreId()).isEqualTo(store.getId());
        assertThat(found.getId().getVariantId()).isEqualTo(variantA.getId());
    }

    @Test
    void findAllForUpdate_returnsOnlyTheRowsThatExistOrderedByVariantId() {
        save(variantB, 3);
        save(variantA, 7);
        entityManager.flush();
        entityManager.clear();

        List<Inventory> locked = inventoryRepository.findAllForUpdate(store.getId(),
                List.of(variantA.getId(), variantB.getId()));

        assertThat(locked).extracting(i -> i.getId().getVariantId())
                .containsExactly(variantA.getId(), variantB.getId());
    }

    @Test
    void existsByIdStoreId_reflectsWhetherTheStoreHasAnyInventoryRow() {
        assertThat(inventoryRepository.existsByIdStoreId(store.getId())).isFalse();

        save(variantA, 1);
        entityManager.flush();

        assertThat(inventoryRepository.existsByIdStoreId(store.getId())).isTrue();
    }

    @Test
    void stockMovement_isSavedAndFoundNewestFirst() {
        StockMovement first = movement(variantA, MovementType.IN, 10, "Nhà cung cấp A");
        stockMovementRepository.save(first);
        entityManager.flush();
        StockMovement second = movement(variantA, MovementType.OUT, -2, null);
        stockMovementRepository.save(second);
        entityManager.flush();
        entityManager.clear();

        List<StockMovement> page = stockMovementRepository
                .findByStoreIdAndVariantIdOrderByCreatedAtDescIdDesc(store.getId(), variantA.getId(), PageRequest.of(0, 10))
                .getContent();

        assertThat(page).extracting(StockMovement::getMovementType)
                .containsExactly(MovementType.OUT, MovementType.IN);
        assertThat(page.getLast().getSupplierName()).isEqualTo("Nhà cung cấp A");
    }

    private void save(ProductVariant variant, int quantity) {
        inventoryRepository.save(new Inventory(storeRepository.getReferenceById(store.getId()),
                variantRepository.getReferenceById(variant.getId()), quantity));
    }

    private StockMovement movement(ProductVariant variant, MovementType type, int change, String supplierName) {
        StockMovement movement = new StockMovement();
        movement.setStore(storeRepository.getReferenceById(store.getId()));
        movement.setVariant(variantRepository.getReferenceById(variant.getId()));
        movement.setMovementType(type);
        movement.setQuantityChange(change);
        movement.setSupplierName(supplierName);
        return movement;
    }

    private static Store store(String name) {
        Store store = new Store();
        store.setName(name);
        store.setAddress("123 Đường Test");
        return store;
    }

    private static ProductVariant variant(Product product, String name) {
        ProductVariant variant = new ProductVariant();
        variant.setProduct(product);
        variant.setVariantName(name);
        variant.setPrice(new BigDecimal("10000000"));
        return variant;
    }
}
