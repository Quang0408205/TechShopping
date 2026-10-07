package com.example.Tech.repository.inventory;

import com.example.Tech.entity.inventory.MovementType;
import com.example.Tech.entity.inventory.StockMovement;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface StockMovementRepository extends JpaRepository<StockMovement, Long> {

    /** What an order took from (OUT) or gave back to (RETURN) the stock, in insertion order. */
    List<StockMovement> findByOrderIdAndMovementTypeOrderById(Long orderId, MovementType movementType);

    /** Movement history of one variant at one store, newest first; who made each movement loaded too. */
    @EntityGraph(attributePaths = "createdBy")
    Page<StockMovement> findByStoreIdAndVariantIdOrderByCreatedAtDescIdDesc(Integer storeId, Long variantId,
                                                                           Pageable pageable);

    /**
     * Totals of the movements of one type in [from, to) — at one store, or every store when storeId is null. The
     * supplier count is case-insensitive and ignores movements without a supplier.
     */
    @Query("select new com.example.Tech.repository.inventory.StockInTotals(coalesce(sum(m.quantityChange), 0L), "
            + "count(m), count(distinct m.variant.id), count(distinct lower(m.supplierName))) "
            + "from StockMovement m where m.movementType = :type and (:storeId is null or m.store.id = :storeId) "
            + "and m.createdAt >= :from and m.createdAt < :to")
    StockInTotals stockInTotals(@Param("type") MovementType type, @Param("storeId") Integer storeId,
                                @Param("from") LocalDateTime from, @Param("to") LocalDateTime to);

    /** Same filter, one row per store that has such movements. */
    @Query("select new com.example.Tech.repository.inventory.StockInStoreTotals(m.store.id, sum(m.quantityChange), "
            + "count(m), count(distinct m.variant.id), max(m.createdAt)) "
            + "from StockMovement m where m.movementType = :type and (:storeId is null or m.store.id = :storeId) "
            + "and m.createdAt >= :from and m.createdAt < :to group by m.store.id")
    List<StockInStoreTotals> stockInTotalsByStore(@Param("type") MovementType type, @Param("storeId") Integer storeId,
                                                  @Param("from") LocalDateTime from, @Param("to") LocalDateTime to);
}
