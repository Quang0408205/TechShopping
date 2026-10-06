package com.example.Tech.repository.inventory;

import com.example.Tech.entity.inventory.MovementType;
import com.example.Tech.entity.inventory.StockMovement;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface StockMovementRepository extends JpaRepository<StockMovement, Long> {

    /** What an order took from (OUT) or gave back to (RETURN) the stock, in insertion order. */
    List<StockMovement> findByOrderIdAndMovementTypeOrderById(Long orderId, MovementType movementType);

    /** Movement history of one variant at one store, newest first; who made each movement loaded too. */
    @EntityGraph(attributePaths = "createdBy")
    Page<StockMovement> findByStoreIdAndVariantIdOrderByCreatedAtDescIdDesc(Integer storeId, Long variantId,
                                                                           Pageable pageable);
}
