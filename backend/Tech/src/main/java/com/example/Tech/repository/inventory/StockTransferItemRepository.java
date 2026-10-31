package com.example.Tech.repository.inventory;

import com.example.Tech.entity.inventory.StockTransferItem;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;

public interface StockTransferItemRepository extends JpaRepository<StockTransferItem, Long> {

    @EntityGraph(attributePaths = {"variant", "variant.product"})
    List<StockTransferItem> findByTransferIdOrderByIdAsc(Long transferId);

    List<StockTransferItem> findByTransferIdInOrderByIdAsc(Collection<Long> transferIds);

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("delete from StockTransferItem i where i.transfer.id = :transferId")
    void deleteByTransferId(@Param("transferId") Long transferId);
}
