package com.example.Tech.repository.order;

import com.example.Tech.entity.order.Order;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;

import java.util.Optional;

public interface OrderRepository extends JpaRepository<Order, Long>, JpaSpecificationExecutor<Order> {

    /** A customer's own order; another user's order gives empty (answered as 404, not 403). */
    Optional<Order> findByIdAndUserId(Long id, Long userId);

    Page<Order> findAllByUserId(Long userId, Pageable pageable);

    /** Staff list: the customer is loaded with the orders (no query per row). */
    @Override
    @EntityGraph(attributePaths = "user")
    Page<Order> findAll(Specification<Order> spec, Pageable pageable);

    @EntityGraph(attributePaths = "user")
    Optional<Order> findWithUserById(Long id);

    /**
     * Locks the order row (SELECT … FOR UPDATE) for a status change or a cancellation, so two concurrent
     * changes (two staff members, or staff and the customer) see each other's result.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select o from Order o where o.id = :id")
    Optional<Order> findByIdForUpdate(Long id);
}
