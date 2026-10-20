package com.learn.tradingmgmt.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.UuidGenerator;

import java.time.Instant;
import java.util.UUID;

/**
 * Common audit columns shared by every "current state" table (orders,
 * transactions, payments, share_transfers, customers, companies,
 * securities, users). Event tables (payment_events, transfer_events,
 * audit_logs, documents/document_versions) are append-only and do NOT
 * extend this class.
 */
@Getter
@Setter
@MappedSuperclass
public abstract class BaseAuditableEntity {

    @Id
    @GeneratedValue
    @UuidGenerator
    @Column(updatable = false, nullable = false)
    private UUID id;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    // TODO: wire to the authenticated principal once Spring Security is added.
    @Column(name = "created_by")
    private UUID createdBy;

    @Column(name = "updated_by")
    private UUID updatedBy;

    @Version
    @Column(nullable = false)
    private Integer version;

    @PrePersist
    protected void onCreate() {
        Instant now = Instant.now();
        this.createdAt = now;
        this.updatedAt = now;
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = Instant.now();
    }
}
