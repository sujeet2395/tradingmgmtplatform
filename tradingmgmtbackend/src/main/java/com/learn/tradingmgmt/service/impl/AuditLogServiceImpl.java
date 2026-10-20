package com.learn.tradingmgmt.service.impl;

import com.learn.tradingmgmt.entity.AuditLog;
import com.learn.tradingmgmt.repository.AuditLogRepository;
import com.learn.tradingmgmt.service.AuditLogService;
import lombok.RequiredArgsConstructor;
import tools.jackson.databind.ObjectMapper;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AuditLogServiceImpl implements AuditLogService {

    private final AuditLogRepository auditLogRepository;
    private final ObjectMapper objectMapper;

    @Override
    @Transactional
    public void record(UUID userId, String action, String entityType, UUID entityId,
                        Object oldValue, Object newValue, String reason, String source) {
        AuditLog log = new AuditLog();
        log.setUserId(userId);
        log.setAction(action);
        log.setEntityType(entityType);
        log.setEntityId(entityId);
        log.setOldValue(toJson(oldValue));
        log.setNewValue(toJson(newValue));
        log.setReason(reason);
        log.setSource(source == null ? "API" : source);
        auditLogRepository.save(log);
    }

    @Override
    public List<AuditLog> getHistory(String entityType, UUID entityId) {
        return auditLogRepository.findByEntityTypeAndEntityIdOrderByOccurredAtAsc(entityType, entityId);
    }

    private String toJson(Object value) {
        if (value == null) return null;
        try {
            return objectMapper.writeValueAsString(value);
        } catch (Exception e) {
            return String.valueOf(value);
        }
    }
}
