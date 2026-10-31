package com.example.Tech.repository.audit;

import com.example.Tech.entity.audit.AuditLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface AuditLogRepository extends JpaRepository<AuditLog, Long>, JpaSpecificationExecutor<AuditLog> {

    /** Entity types that have a row, for the filter of the admin page. */
    @Query("select distinct a.entityType from AuditLog a order by a.entityType")
    List<String> findEntityTypes();
}
