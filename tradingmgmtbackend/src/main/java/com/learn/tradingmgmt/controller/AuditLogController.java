package com.learn.tradingmgmt.controller;

import com.learn.tradingmgmt.entity.AuditLog;
import com.learn.tradingmgmt.service.AuditLogService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/audit")
@RequiredArgsConstructor
public class AuditLogController {

    private final AuditLogService auditLogService;

    @GetMapping("/{entityType}/{entityId}")
    public List<AuditLog> history(@PathVariable String entityType, @PathVariable UUID entityId) {
        return auditLogService.getHistory(entityType, entityId);
    }
}
