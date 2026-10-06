package com.example.Tech.repository.product;

import com.example.Tech.entity.product.Attribute;
import com.example.Tech.entity.product.Category;
import com.example.Tech.entity.product.CategoryAttribute;
import jakarta.persistence.EntityManager;
import org.hibernate.Hibernate;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Checks the CategoryAttribute mapping (composite key, like PromotionProduct) against the real PostgreSQL
 * test database (techshopping_test). Every test is rolled back. Schema only for now (v3 survey, 2026-10-05);
 * no service uses this table yet.
 */
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ActiveProfiles("test")
class CategoryAttributeRepositoryTest {

    @Autowired
    private CategoryAttributeRepository categoryAttributeRepository;

    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired
    private AttributeRepository attributeRepository;

    @Autowired
    private EntityManager entityManager;

    private Category category;
    private Attribute color;
    private Attribute storage;

    @BeforeEach
    void setUp() {
        category = categoryRepository.save(category("CA Test Điện thoại"));
        color = attributeRepository.save(attribute("CA Test Màu"));
        storage = attributeRepository.save(attribute("CA Test Dung lượng"));
        entityManager.flush();
        entityManager.clear();
    }

    @Test
    void save_setsCompositeKeyAndDefaults() {
        CategoryAttribute saved = categoryAttributeRepository.save(
                new CategoryAttribute(category, color, true, true, 1));
        entityManager.flush();
        entityManager.clear();

        CategoryAttribute found = categoryAttributeRepository.findById(saved.getId()).orElseThrow();
        assertThat(found.getId().getCategoryId()).isEqualTo(category.getId());
        assertThat(found.getId().getAttributeId()).isEqualTo(color.getId());
        assertThat(found.getRequired()).isTrue();
        assertThat(found.getFilterable()).isTrue();
        assertThat(found.getDisplayOrder()).isEqualTo(1);
    }

    @Test
    void findAllByCategoryId_ordersByDisplayOrder_andLoadsTheAttribute() {
        categoryAttributeRepository.save(new CategoryAttribute(category, storage, true, true, 2));
        categoryAttributeRepository.save(new CategoryAttribute(category, color, false, true, 1));
        entityManager.flush();
        entityManager.clear();

        List<CategoryAttribute> found = categoryAttributeRepository.findAllByCategoryId(category.getId());

        assertThat(found).extracting(ca -> ca.getAttribute().getName())
                .containsExactly("CA Test Màu", "CA Test Dung lượng");
        assertThat(found.getFirst().getRequired()).isFalse();
        assertThat(Hibernate.isInitialized(found.getFirst().getAttribute())).isTrue();
    }

    @Test
    void save_sameCategoryAndAttributeTwice_mergesIntoTheSameRow() {
        // the id is assigned (not generated), so a second save() merges into the existing row instead of
        // inserting a duplicate — the same behaviour as PromotionProduct's composite key.
        categoryAttributeRepository.save(new CategoryAttribute(category, color, true, true, 1));
        entityManager.flush();
        entityManager.clear();

        categoryAttributeRepository.save(new CategoryAttribute(category, color, false, false, 2));
        entityManager.flush();
        entityManager.clear();

        assertThat(categoryAttributeRepository.findAllByCategoryId(category.getId())).hasSize(1)
                .first().extracting(CategoryAttribute::getRequired).isEqualTo(false);
    }

    private static Category category(String name) {
        Category category = new Category();
        category.setName(name);
        category.setSlug("test-category-attributes");
        return category;
    }

    private static Attribute attribute(String name) {
        Attribute attribute = new Attribute();
        attribute.setName(name);
        return attribute;
    }
}
