package com.example.Tech.repository.product;

import com.example.Tech.entity.product.ProductSpecification;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface ProductSpecificationRepository extends JpaRepository<ProductSpecification, Long> {

    /** Specifications of soft-deleted products are treated as not found. */
    @EntityGraph(attributePaths = "product")
    Optional<ProductSpecification> findByIdAndProductDeletedAtIsNull(Long id);

    /** NULL spec orders sort last (PostgreSQL default for ASC). */
    List<ProductSpecification> findAllByProductIdOrderBySpecOrderAscIdAsc(Long productId);

    /** Excel import replaces the whole specification list of a product. */
    void deleteAllByProductId(Long productId);

    /** Specification names used most often in a category (template for the admin product form). */
    @Query("select s.specName from ProductSpecification s where s.product.category.id = :categoryId "
            + "and s.product.deletedAt is null and s.variantId is null group by s.specName order by count(s) desc, s.specName")
    List<String> findCommonNamesByCategoryId(@Param("categoryId") Integer categoryId, Pageable pageable);
}
