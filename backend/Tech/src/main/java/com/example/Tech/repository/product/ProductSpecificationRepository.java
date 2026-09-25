package com.example.Tech.repository.product;

import com.example.Tech.entity.product.ProductSpecification;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ProductSpecificationRepository extends JpaRepository<ProductSpecification, Long> {

    /** Specifications of soft-deleted products are treated as not found. */
    @EntityGraph(attributePaths = "product")
    Optional<ProductSpecification> findByIdAndProductDeletedAtIsNull(Long id);

    /** NULL spec orders sort last (PostgreSQL default for ASC). */
    List<ProductSpecification> findAllByProductIdOrderBySpecOrderAscIdAsc(Long productId);
}
