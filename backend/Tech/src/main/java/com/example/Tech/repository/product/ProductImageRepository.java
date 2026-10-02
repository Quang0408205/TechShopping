package com.example.Tech.repository.product;

import com.example.Tech.entity.product.ProductImage;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface ProductImageRepository extends JpaRepository<ProductImage, Long> {

    /** Images of soft-deleted products are treated as not found. */
    @EntityGraph(attributePaths = "product")
    Optional<ProductImage> findByIdAndProductDeletedAtIsNull(Long id);

    /** NULL display orders sort last (PostgreSQL default for ASC). */
    List<ProductImage> findAllByProductIdOrderByDisplayOrderAscIdAsc(Long productId);

    List<ProductImage> findAllByProductIdAndPrimaryTrue(Long productId);

    /**
     * Images of several products in one query, best first per product: primary, then display order
     * (NULL last), then id. Used to show one image per cart line.
     */
    @Query("select i from ProductImage i where i.product.id in :productIds "
            + "order by i.product.id, case when i.primary = true then 0 else 1 end, i.displayOrder asc nulls last, i.id")
    List<ProductImage> findAllByProductIdInBestFirst(Collection<Long> productIds);
}
