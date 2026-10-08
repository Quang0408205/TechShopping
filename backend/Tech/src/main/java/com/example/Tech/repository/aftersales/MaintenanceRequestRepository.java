package com.example.Tech.repository.aftersales;

import com.example.Tech.entity.aftersales.MaintenanceRequest;
import com.example.Tech.entity.aftersales.ServiceRequestStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface MaintenanceRequestRepository extends JpaRepository<MaintenanceRequest, Long> {

    boolean existsByOrderItem_IdAndStatusIn(Long orderItemId, Collection<ServiceRequestStatus> statuses);

    List<MaintenanceRequest> findAllByOrderItem_IdInAndStatusIn(Collection<Long> orderItemIds,
                                                                Collection<ServiceRequestStatus> statuses);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select r from MaintenanceRequest r where r.id = :id")
    Optional<MaintenanceRequest> findByIdForUpdate(Long id);

    @EntityGraph(attributePaths = {"user", "assignedTo", "orderItem.variant.product", "orderItem.order.store"})
    List<MaintenanceRequest> findAllByIdIn(Collection<Long> ids);
}
