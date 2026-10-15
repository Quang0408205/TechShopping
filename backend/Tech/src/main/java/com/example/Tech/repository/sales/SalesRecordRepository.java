package com.example.Tech.repository.sales;

import com.example.Tech.entity.sales.SalesRecord;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface SalesRecordRepository extends JpaRepository<SalesRecord, Long> {

    Optional<SalesRecord> findByOrder_Id(Long orderId);
}
