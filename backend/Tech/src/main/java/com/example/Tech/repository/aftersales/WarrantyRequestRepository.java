package com.example.Tech.repository.aftersales;

import com.example.Tech.entity.aftersales.ServiceRequestStatus;
import com.example.Tech.entity.aftersales.WarrantyRequest;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface WarrantyRequestRepository extends JpaRepository<WarrantyRequest, Long> {

    boolean existsByWarranty_IdAndStatusIn(Long warrantyId, Collection<ServiceRequestStatus> statuses);

    List<WarrantyRequest> findAllByWarranty_IdInAndStatusIn(Collection<Long> warrantyIds,
                                                            Collection<ServiceRequestStatus> statuses);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select r from WarrantyRequest r where r.id = :id")
    Optional<WarrantyRequest> findByIdForUpdate(Long id);

    @EntityGraph(attributePaths = {"user", "assignedTo", "warranty.orderItem.variant.product",
            "warranty.orderItem.order.store"})
    List<WarrantyRequest> findAllByIdIn(Collection<Long> ids);
}
