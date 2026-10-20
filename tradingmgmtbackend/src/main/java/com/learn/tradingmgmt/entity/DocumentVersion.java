package com.learn.tradingmgmt.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.UuidGenerator;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.learn.tradingmgmt.entity.enums.DocumentVerificationStatus;

import java.time.Instant;
import java.util.UUID;

/**
 * Append-only. Never overwrite or delete a row here — Document.currentVersion
 * just points at the latest one.
 */
@Entity
@Table(name = "document_versions",
       uniqueConstraints = @UniqueConstraint(columnNames = {"document_id", "version_number"}))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class DocumentVersion {

    @Id
    @GeneratedValue
    @UuidGenerator
    private UUID id;

    @JsonIgnore
    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "document_id", nullable = false)
    private Document document;

    @Column(name = "version_number", nullable = false)
    private Integer versionNumber;

    @Column(name = "file_name", nullable = false)
    private String fileName;

    @JsonIgnore
    @Column(name = "storage_path", nullable = false, columnDefinition = "text")
    private String storagePath;

    @Enumerated(EnumType.STRING)
    @Column(name = "verification_status", nullable = false, length = 20)
    private DocumentVerificationStatus verificationStatus = DocumentVerificationStatus.PENDING;
    
    @Column(name = "verified_by")
    private UUID verifiedBy;
    
    @Column(name = "verified_at")
    private Instant verifiedAt;
    
    @Column(name = "verification_note", columnDefinition = "text")
    private String verificationNote;

    @Column(name = "content_hash")
    private String contentHash;

    @Column(name = "uploaded_at", nullable = false)
    private Instant uploadedAt;

    @Column(name = "uploaded_by")
    private UUID uploadedBy;

    @PrePersist
    void onCreate() {
        this.uploadedAt = Instant.now();
    }
}
