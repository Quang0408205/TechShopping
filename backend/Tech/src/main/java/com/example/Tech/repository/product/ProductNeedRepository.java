package com.example.Tech.repository.product;

import com.example.Tech.entity.product.ProductNeed;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface ProductNeedRepository extends JpaRepository<ProductNeed, Long> {

    @EntityGraph(attributePaths = "need")
    List<ProductNeed> findAllByProductId(Long productId);

    @Modifying
    @Query("delete from ProductNeed n where n.productId = :productId")
    void deleteAllByProductId(@Param("productId") Long productId);

    /** [needId, productCount] of the products on sale in a category. */
    @Query("select n.need.id, count(distinct n.productId) from ProductNeed n, Product p "
            + "where n.productId = p.id and p.category.id = :categoryId and p.active = true and p.deletedAt is null "
            + "group by n.need.id")
    List<Object[]> countByCategory(@Param("categoryId") Integer categoryId);
}
