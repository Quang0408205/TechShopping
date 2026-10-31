package com.example.Tech.repository.inventory;

import com.example.Tech.entity.inventory.Supplier;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface SupplierRepository extends JpaRepository<Supplier, Integer>, JpaSpecificationExecutor<Supplier> {

    @Query("select count(s) > 0 from Supplier s where lower(s.name) = lower(:name) and (:exceptId is null or s.id <> :exceptId)")
    boolean existsByNameIgnoreCase(@Param("name") String name, @Param("exceptId") Integer exceptId);
}
