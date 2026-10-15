package com.example.Tech.repository.payment;

import com.example.Tech.entity.payment.InstallmentOrder;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface InstallmentOrderRepository extends JpaRepository<InstallmentOrder, Long>,
        JpaSpecificationExecutor<InstallmentOrder> {

    Optional<InstallmentOrder> findByOrderId(Long orderId);

    /** Plans of a page of orders in one query. */
    List<InstallmentOrder> findAllByOrderIdIn(Collection<Long> orderIds);

    /** Staff list: the order and its customer are loaded with the plans (no query per row). */
    @Override
    @EntityGraph(attributePaths = {"order", "order.user"})
    Page<InstallmentOrder> findAll(Specification<InstallmentOrder> spec, Pageable pageable);

    @EntityGraph(attributePaths = {"order", "order.user"})
    Optional<InstallmentOrder> findWithOrderById(Long id);

    /** Scalar: does not load the plan, so it can be read fresh after the order row is locked. */
    @Query("select p.order.id from InstallmentOrder p where p.id = :id")
    Optional<Long> findOrderIdById(Long id);
}
