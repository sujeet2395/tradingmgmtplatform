package com.learn.tradingmgmt.entity;

import com.learn.tradingmgmt.entity.enums.DocumentCategory;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.UuidGenerator;

import java.time.Instant;
import java.util.UUID;

/**
 * Polymorphic association via relatedEntityType/relatedEntityId (rather
 * than a nullable FK per entity type) so the same model covers KYC,
 * cheque evidence, transfer forms, etc. without a schema change per type.
 */
@Entity
@Table(name = "documents")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Document {

    @Id
    @GeneratedValue
    @UuidGenerator
    private UUID id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private DocumentCategory category;

    @Column(name = "related_entity_type", nullable = false)
    private String relatedEntityType;

    @Column(name = "related_entity_id", nullable = false)
    private UUID relatedEntityId;

    @OneToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "current_version_id")
    private DocumentVersion currentVersion;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "created_by")
    private UUID createdBy;

    @PrePersist
    void onCreate() {
        this.createdAt = Instant.now();
    }
}
