package com.example.Tech.repository.voucher;

import com.example.Tech.entity.voucher.Voucher;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface VoucherRepository extends JpaRepository<Voucher, Long>, JpaSpecificationExecutor<Voucher> {

    /** Codes are stored upper case; callers pass the normalised code. */
    Optional<Voucher> findByCode(String code);

    boolean existsByCode(String code);

    /** Row lock while a use is counted or given back (two checkouts never both take the last use). */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select v from Voucher v where v.id = :id")
    Optional<Voucher> findByIdForUpdate(Long id);

    /** Vouchers customers can pick now, ending soonest first. */
    @Query("select v from Voucher v where v.active = true and v.startsAt <= :now and v.endsAt > :now "
            + "order by v.endsAt, v.id")
    List<Voucher> findRunning(LocalDateTime now);
}
