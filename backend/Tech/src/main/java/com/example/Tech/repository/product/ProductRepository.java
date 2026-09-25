package com.example.Tech.repository.product;

import com.example.Tech.entity.product.Product;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.Optional;

public interface ProductRepository extends JpaRepository<Product, Long>, JpaSpecificationExecutor<Product> {

    @Override
    @EntityGraph(attributePaths = {"category", "brand"})
    Page<Product> findAll(Specification<Product> spec, Pageable pageable);

    @EntityGraph(attributePaths = {"category", "brand"})
    Optional<Product> findByIdAndDeletedAtIsNull(Long id);

    // Uniqueness checks include soft-deleted rows: the DB unique constraints still apply to them
    boolean existsBySlug(String slug);

    boolean existsBySlugAndIdNot(String slug, Long id);

    boolean existsBySku(String sku);

    boolean existsBySkuAndIdNot(String sku, Long id);

    // Soft-deleted products still reference their category/brand through the FK
    boolean existsByCategoryId(Integer categoryId);

    boolean existsByBrandId(Integer brandId);
}
