package com.example.Tech.repository.review;

import com.example.Tech.entity.review.Review;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface ReviewRepository extends JpaRepository<Review, Long>, JpaSpecificationExecutor<Review> {

    /** Admin list (ReviewFilterSpecifications): author, product and the hiding admin loaded with the rows. */
    @Override
    @EntityGraph(attributePaths = {"user", "product", "hiddenBy"})
    Page<Review> findAll(Specification<Review> spec, Pageable pageable);

    Optional<Review> findByUser_IdAndProduct_Id(Long userId, Long productId);

    boolean existsByUser_IdAndProduct_Id(Long userId, Long productId);

    List<Review> findAllByProduct_IdAndHiddenFalseOrderByCreatedAtDesc(Long productId);

    /** The caller's own review (any visibility) with its product; another account's id behaves as missing. */
    @EntityGraph(attributePaths = {"user", "product"})
    Optional<Review> findByIdAndUser_Id(Long id, Long userId);

    /** The caller's reviews, newest first, optionally of one product (null = all). */
    @EntityGraph(attributePaths = {"user", "product"})
    @Query("select r from Review r where r.user.id = :userId and (:productId is null or r.product.id = :productId) "
            + "order by r.createdAt desc, r.id desc")
    List<Review> findMine(@Param("userId") Long userId, @Param("productId") Long productId);

    /**
     * Visible reviews of a product, newest first, optionally only one star level and / or only those with photos.
     * The pageable's sort is not used (the order is fixed here).
     */
    @EntityGraph(attributePaths = "user")
    @Query(value = "select r from Review r where r.product.id = :productId and r.hidden = false "
            + "and (:rating is null or r.rating = :rating) "
            + "and (:withImages = false or exists (select 1 from ReviewImage i where i.review = r)) "
            + "order by r.createdAt desc, r.id desc",
            countQuery = "select count(r) from Review r where r.product.id = :productId and r.hidden = false "
                    + "and (:rating is null or r.rating = :rating) "
                    + "and (:withImages = false or exists (select 1 from ReviewImage i where i.review = r))")
    Page<Review> findVisible(@Param("productId") Long productId, @Param("rating") Short rating,
                             @Param("withImages") boolean withImages, Pageable pageable);

    /** [rating, count] of the product's visible reviews, one row per star level present. */
    @Query("select r.rating, count(r) from Review r where r.product.id = :productId and r.hidden = false "
            + "group by r.rating")
    List<Object[]> countVisibleByRating(@Param("productId") Long productId);

    @Query("select count(r) from Review r where r.product.id = :productId and r.hidden = false "
            + "and exists (select 1 from ReviewImage i where i.review = r)")
    long countVisibleWithImages(@Param("productId") Long productId);
}
