package com.example.Tech.repository.promotion;

import com.example.Tech.entity.promotion.PromotionProduct;
import com.example.Tech.entity.promotion.PromotionProductId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface PromotionProductRepository extends JpaRepository<PromotionProduct, PromotionProductId> {

    /** The products of several promotions in one query, products loaded too. */
    @Query("""
            select pp from PromotionProduct pp
            join fetch pp.product
            where pp.id.promotionId in :promotionIds
            order by pp.id.promotionId, pp.id.productId
            """)
    List<PromotionProduct> findAllWithProductByPromotionIdIn(@Param("promotionIds") List<Long> promotionIds);

    /**
     * Product ids (among the given candidates) that already belong to some OTHER active promotion whose
     * date range overlaps [windowStart, windowEnd]. Used to reject a create/update before it is persisted
     * (a product may not be in two simultaneously-active promotions). excludePromotionId lets an update
     * compare against every promotion except itself; pass -1 on create (no id to exclude yet).
     */
    @Query("""
            select distinct pp.id.productId from PromotionProduct pp
            where pp.id.productId in :productIds
              and pp.promotion.active = true
              and pp.promotion.id <> :excludePromotionId
              and pp.promotion.startDate < :windowEnd
              and pp.promotion.endDate > :windowStart
            """)
    List<Long> findOverlappingProductIds(@Param("productIds") List<Long> productIds,
                                          @Param("windowStart") LocalDateTime windowStart,
                                          @Param("windowEnd") LocalDateTime windowEnd,
                                          @Param("excludePromotionId") Long excludePromotionId);

    /**
     * The one active promotion (if any) currently covering each of the given products, as of now. One
     * query for a whole page of products avoids N+1 when rendering a product listing.
     */
    @Query("""
            select pp from PromotionProduct pp
            join fetch pp.promotion
            where pp.id.productId in :productIds
              and pp.promotion.active = true
              and pp.promotion.startDate <= :now
              and pp.promotion.endDate > :now
            """)
    List<PromotionProduct> findActiveForProducts(@Param("productIds") List<Long> productIds,
                                                   @Param("now") LocalDateTime now);
}
