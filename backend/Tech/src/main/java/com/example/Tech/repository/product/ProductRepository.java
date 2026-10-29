package com.example.Tech.repository.product;

import com.example.Tech.entity.product.Product;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface ProductRepository extends JpaRepository<Product, Long>, JpaSpecificationExecutor<Product> {

    @Override
    @EntityGraph(attributePaths = {"category", "brand"})
    Page<Product> findAll(Specification<Product> spec, Pageable pageable);

    @EntityGraph(attributePaths = {"category", "brand"})
    Optional<Product> findByIdAndDeletedAtIsNull(Long id);

    /**
     * Locks the product rows (SELECT … FOR UPDATE, in id order so two callers cannot deadlock). Promotion
     * writes take this lock before their overlap check, so two admins cannot put the same product into two
     * overlapping promotions at the same time.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select p from Product p where p.id in :ids order by p.id")
    List<Product> findAllByIdInForUpdate(@Param("ids") List<Long> ids);

    // Uniqueness checks include soft-deleted rows: the DB unique constraints still apply to them
    boolean existsBySlug(String slug);

    boolean existsBySlugAndIdNot(String slug, Long id);

    boolean existsBySku(String sku);

    /** Excel import: products are matched by their SKU ("Mã SP"), soft-deleted ones included. */
    Optional<Product> findBySku(String sku);

    boolean existsBySkuAndIdNot(String sku, Long id);

    /** Products on sale (not deleted, not hidden) per category: [categoryId, count]. */
    @Query("select p.category.id, count(p) from Product p where p.deletedAt is null "
            + "and (p.active is null or p.active = true) and p.category is not null group by p.category.id")
    List<Object[]> countOnSaleByCategory();

    // Soft-deleted products still reference their category/brand through the FK
    boolean existsByCategoryId(Integer categoryId);

    // Plan v3: deleting a category together with its products (CategoryServiceImpl.delete with withProducts)

    /** Products of the category, soft-deleted ones included (they still hold the foreign key). */
    @Query(value = "select count(*) from products where category_id = :categoryId", nativeQuery = true)
    long countAllRowsByCategoryId(@Param("categoryId") Integer categoryId);

    /** Products of the category with a sales / stock / review history, which must never be removed. */
    @Query(value = "select count(*) from products p where p.category_id = :categoryId and ("
            + "exists (select 1 from product_variants v join order_items oi on oi.variant_id = v.variant_id where v.product_id = p.product_id) "
            + "or exists (select 1 from product_variants v join inventory i on i.variant_id = v.variant_id where v.product_id = p.product_id) "
            + "or exists (select 1 from product_variants v join stock_movements m on m.variant_id = v.variant_id where v.product_id = p.product_id) "
            + "or exists (select 1 from reviews r where r.product_id = p.product_id))", nativeQuery = true)
    long countWithHistoryByCategoryId(@Param("categoryId") Integer categoryId);

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query(value = "delete from cart_items where variant_id in (select v.variant_id from product_variants v "
            + "join products p on p.product_id = v.product_id where p.category_id = :categoryId)", nativeQuery = true)
    int deleteCartLinesByCategoryId(@Param("categoryId") Integer categoryId);

    /** Analytics rows without ON DELETE CASCADE (all three tables are empty today, but must not block the delete). */
    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query(value = "delete from recommendations where product_id in "
            + "(select product_id from products where category_id = :categoryId)", nativeQuery = true)
    int deleteRecommendationsByCategoryId(@Param("categoryId") Integer categoryId);

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query(value = "delete from user_interactions where product_id in "
            + "(select product_id from products where category_id = :categoryId)", nativeQuery = true)
    int deleteInteractionsByCategoryId(@Param("categoryId") Integer categoryId);

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query(value = "delete from product_relations where product_id in (select product_id from products where category_id = :categoryId) "
            + "or related_product_id in (select product_id from products where category_id = :categoryId)", nativeQuery = true)
    int deleteRelationsByCategoryId(@Param("categoryId") Integer categoryId);

    /** Images, variants (+ their attribute values), specifications and promotion lines go with them (ON DELETE CASCADE). */
    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query(value = "delete from products where category_id = :categoryId", nativeQuery = true)
    int deleteAllRowsByCategoryId(@Param("categoryId") Integer categoryId);

    boolean existsByBrandId(Integer brandId);
}
