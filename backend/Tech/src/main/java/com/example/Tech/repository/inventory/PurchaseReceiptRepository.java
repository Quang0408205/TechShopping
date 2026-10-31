package com.example.Tech.repository.inventory;

import com.example.Tech.entity.inventory.PurchaseReceipt;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface PurchaseReceiptRepository extends JpaRepository<PurchaseReceipt, Long>,
        JpaSpecificationExecutor<PurchaseReceipt> {

    @EntityGraph(attributePaths = {"supplier", "store", "createdBy", "completedBy"})
    Optional<PurchaseReceipt> findWithDetailsById(Long id);

    /** Locks the receipt so completing and cancelling it cannot overlap. */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select r from PurchaseReceipt r where r.id = :id")
    Optional<PurchaseReceipt> findByIdForUpdate(@Param("id") Long id);

    boolean existsBySupplierId(Integer supplierId);
}
