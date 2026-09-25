package com.example.Tech.repository.product;

import com.example.Tech.entity.product.ProductImage;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ProductImageRepository extends JpaRepository<ProductImage, Long> {

    /** Images of soft-deleted products are treated as not found. */
    @EntityGraph(attributePaths = "product")
    Optional<ProductImage> findByIdAndProductDeletedAtIsNull(Long id);

    /** NULL display orders sort last (PostgreSQL default for ASC). */
    List<ProductImage> findAllByProductIdOrderByDisplayOrderAscIdAsc(Long productId);

    List<ProductImage> findAllByProductIdAndPrimaryTrue(Long productId);
}
