package com.example.Tech.repository.inventory;

import com.example.Tech.entity.inventory.StockTransfer;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface StockTransferRepository extends JpaRepository<StockTransfer, Long>,
        JpaSpecificationExecutor<StockTransfer> {

    @EntityGraph(attributePaths = {"fromStore", "toStore", "createdBy", "sentBy", "receivedBy", "cancelledBy"})
    Optional<StockTransfer> findWithDetailsById(Long id);

    /** Locks the transfer so sending, receiving and cancelling it cannot overlap. */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select t from StockTransfer t where t.id = :id")
    Optional<StockTransfer> findByIdForUpdate(@Param("id") Long id);
}
