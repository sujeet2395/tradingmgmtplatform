package com.learn.tradingmgmt.repository;

import com.learn.tradingmgmt.entity.AuditLog;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface AuditLogRepository extends JpaRepository<AuditLog, UUID> {
    List<AuditLog> findByEntityTypeAndEntityIdOrderByOccurredAtAsc(String entityType, UUID entityId);
}
