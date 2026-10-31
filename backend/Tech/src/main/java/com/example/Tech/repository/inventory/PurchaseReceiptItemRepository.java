package com.example.Tech.repository.inventory;

import com.example.Tech.entity.inventory.PurchaseReceiptItem;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;

public interface PurchaseReceiptItemRepository extends JpaRepository<PurchaseReceiptItem, Long> {

    @EntityGraph(attributePaths = {"variant", "variant.product"})
    List<PurchaseReceiptItem> findByReceiptIdOrderByIdAsc(Long receiptId);

    List<PurchaseReceiptItem> findByReceiptIdInOrderByIdAsc(Collection<Long> receiptIds);

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("delete from PurchaseReceiptItem i where i.receipt.id = :receiptId")
    void deleteByReceiptId(@Param("receiptId") Long receiptId);
}
