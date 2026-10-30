package com.example.Tech.repository.payment;

import com.example.Tech.entity.payment.InstallmentFeeRate;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface InstallmentFeeRateRepository extends JpaRepository<InstallmentFeeRate, InstallmentFeeRate.Key> {

    @Query("select r from InstallmentFeeRate r where r.id.partnerCode = :partnerCode order by r.id.numMonths")
    List<InstallmentFeeRate> findAllByPartnerCode(String partnerCode);

    @Query("select r from InstallmentFeeRate r order by r.id.partnerCode, r.id.numMonths")
    List<InstallmentFeeRate> findAllSorted();

    @Query("select r from InstallmentFeeRate r where r.id.partnerCode = :partnerCode and r.id.numMonths = :numMonths")
    Optional<InstallmentFeeRate> findOne(String partnerCode, int numMonths);

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("delete from InstallmentFeeRate r where r.id.partnerCode = :partnerCode")
    void deleteAllByPartnerCode(String partnerCode);
}
