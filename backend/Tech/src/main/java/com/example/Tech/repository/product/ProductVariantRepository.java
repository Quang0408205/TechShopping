package com.example.Tech.repository.product;

import com.example.Tech.entity.product.ProductVariant;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ProductVariantRepository extends JpaRepository<ProductVariant, Long> {

    /** Variants of soft-deleted products are treated as not found. */
    @EntityGraph(attributePaths = "product")
    Optional<ProductVariant> findByIdAndProductDeletedAtIsNull(Long id);

    @EntityGraph(attributePaths = "product")
    List<ProductVariant> findAllByProductIdOrderByIdAsc(Long productId);

    boolean existsBySkuVariant(String skuVariant);

    boolean existsBySkuVariantAndIdNot(String skuVariant, Long id);
}
