package com.example.Tech.repository.product;

import com.example.Tech.entity.product.ProductFilterValue;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface ProductFilterValueRepository extends JpaRepository<ProductFilterValue, Long> {

    @Modifying
    @Query("delete from ProductFilterValue f where f.productId = :productId")
    void deleteAllByProductId(@Param("productId") Long productId);

    @Modifying
    @Query("delete from ProductFilterValue f")
    void deleteEverything();

    /** [groupKey, label, minSortOrder, productCount] of the products on sale in a category. */
    @Query("select f.groupKey, f.label, min(f.sortOrder), count(distinct f.productId) from ProductFilterValue f, Product p "
            + "where f.productId = p.id and p.category.id = :categoryId and p.active = true and p.deletedAt is null "
            + "group by f.groupKey, f.label")
    List<Object[]> countByCategory(@Param("categoryId") Integer categoryId);
}
