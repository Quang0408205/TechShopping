package com.example.Tech.repository.voucher;

import com.example.Tech.entity.voucher.VoucherUsage;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface VoucherUsageRepository extends JpaRepository<VoucherUsage, Long> {

    /** Uses of a voucher by one customer that still count (order not cancelled). */
    long countByVoucherIdAndUserIdAndReleasedAtIsNull(Long voucherId, Long userId);

    /** Same, for several vouchers at once: [voucherId, count]. */
    @Query("select u.voucher.id, count(u) from VoucherUsage u where u.user.id = :userId and u.releasedAt is null "
            + "and u.voucher.id in :voucherIds group by u.voucher.id")
    List<Object[]> countActiveByUser(Long userId, Collection<Long> voucherIds);

    Optional<VoucherUsage> findByOrderId(Long orderId);

    /** Any order ever used it (cancelled ones too): then the code is locked and the voucher cannot be deleted. */
    boolean existsByVoucherId(Long voucherId);

    @EntityGraph(attributePaths = {"order", "user"})
    Page<VoucherUsage> findAllByVoucherId(Long voucherId, Pageable pageable);
}
