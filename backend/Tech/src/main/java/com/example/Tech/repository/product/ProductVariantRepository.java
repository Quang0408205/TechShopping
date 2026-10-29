package com.example.Tech.repository.product;

import com.example.Tech.entity.product.ProductVariant;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.List;
import java.util.Optional;

public interface ProductVariantRepository extends JpaRepository<ProductVariant, Long>,
        JpaSpecificationExecutor<ProductVariant> {

    boolean existsByIdAndProductId(Long id, Long productId);


    /** All-stores inventory page (InventoryFilterSpecifications.stockedVariants): product loaded with the rows. */
    @Override
    @EntityGraph(attributePaths = "product")
    Page<ProductVariant> findAll(Specification<ProductVariant> spec, Pageable pageable);

    /** Variants of soft-deleted products are treated as not found. */
    @EntityGraph(attributePaths = "product")
    Optional<ProductVariant> findByIdAndProductDeletedAtIsNull(Long id);

    @EntityGraph(attributePaths = "product")
    List<ProductVariant> findAllByProductIdOrderByIdAsc(Long productId);

    /** Product cards: the variants of a page of products, one query. */
    List<ProductVariant> findAllByProductIdInOrderByIdAsc(java.util.Collection<Long> productIds);

    boolean existsBySkuVariant(String skuVariant);

    /** Excel imports: variants are matched by their SKU, with the product loaded. */
    @EntityGraph(attributePaths = "product")
    Optional<ProductVariant> findBySkuVariant(String skuVariant);

    boolean existsBySkuVariantAndIdNot(String skuVariant, Long id);
}
