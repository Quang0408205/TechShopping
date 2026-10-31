package com.example.Tech.repository.order;

import com.example.Tech.entity.order.ShippingFeeRule;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface ShippingFeeRuleRepository extends JpaRepository<ShippingFeeRule, Integer> {

    @Query("select r from ShippingFeeRule r where lower(r.provinceName) = lower(:name)")
    Optional<ShippingFeeRule> findByProvinceNameIgnoreCase(@Param("name") String name);

    @Query("select count(r) > 0 from ShippingFeeRule r where lower(r.provinceName) = lower(:name) and (:exceptId is null or r.id <> :exceptId)")
    boolean existsByProvinceNameIgnoreCase(@Param("name") String name, @Param("exceptId") Integer exceptId);

    List<ShippingFeeRule> findAllByOrderByProvinceNameAsc();
}
