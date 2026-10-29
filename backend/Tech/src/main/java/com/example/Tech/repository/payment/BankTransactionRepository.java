package com.example.Tech.repository.payment;

import com.example.Tech.entity.payment.BankTransaction;
import com.example.Tech.entity.payment.BankTransactionStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.Optional;

public interface BankTransactionRepository extends JpaRepository<BankTransaction, Long> {

    boolean existsByExternalId(String externalId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select t from BankTransaction t where t.id = :id")
    Optional<BankTransaction> findByIdForUpdate(@Param("id") Long id);

    @EntityGraph(attributePaths = "order")
    Page<BankTransaction> findAllBy(Pageable pageable);

    @EntityGraph(attributePaths = "order")
    Page<BankTransaction> findAllByStatusIn(Collection<BankTransactionStatus> statuses, Pageable pageable);

    long countByStatusIn(Collection<BankTransactionStatus> statuses);
}
