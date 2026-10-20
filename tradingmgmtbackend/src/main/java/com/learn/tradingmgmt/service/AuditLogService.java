package com.learn.tradingmgmt.service;

import com.learn.tradingmgmt.entity.AuditLog;

import java.util.List;
import java.util.UUID;

public interface AuditLogService {
    /**
     * @param oldValue / newValue any object — serialized to JSON text before storage.
     * @param source   e.g. "API", "UI", "SYSTEM"
     */
    void record(UUID userId, String action, String entityType, UUID entityId,
                Object oldValue, Object newValue, String reason, String source);

    List<AuditLog> getHistory(String entityType, UUID entityId);
}
