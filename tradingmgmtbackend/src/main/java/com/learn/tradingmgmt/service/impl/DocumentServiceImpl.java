package com.learn.tradingmgmt.service.impl;

import com.learn.tradingmgmt.dto.request.DocumentUploadRequest;
import com.learn.tradingmgmt.entity.Document;
import com.learn.tradingmgmt.entity.DocumentVersion;
import com.learn.tradingmgmt.entity.enums.DocumentCategory;
import com.learn.tradingmgmt.entity.enums.DocumentVerificationStatus;
import com.learn.tradingmgmt.exception.ResourceNotFoundException;
import com.learn.tradingmgmt.repository.DocumentRepository;
import com.learn.tradingmgmt.repository.DocumentVersionRepository;
import com.learn.tradingmgmt.service.AuditLogService;
import com.learn.tradingmgmt.service.DocumentService;
import lombok.RequiredArgsConstructor;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.time.Instant;
import java.util.HexFormat;
import java.util.List;
import java.util.UUID;
import java.nio.file.*;

import java.security.MessageDigest;

/**
 * Documents are never overwritten — addVersion() always inserts a new
 * DocumentVersion row and repoints currentVersion at it.
 */
@Service
@RequiredArgsConstructor
public class DocumentServiceImpl implements DocumentService {

    private final DocumentRepository documentRepository;
    private final DocumentVersionRepository documentVersionRepository;
    private final AuditLogService auditLogService;
    @Value("${app.storage.dir:./uploads}")
    private String storageDir;

    @Override
    @Transactional
    public Document upload(DocumentUploadRequest request, UUID actingUserId) {
        Document document = new Document();
        document.setCategory(request.category());
        document.setRelatedEntityType(request.relatedEntityType());
        document.setRelatedEntityId(request.relatedEntityId());
        document.setCreatedBy(actingUserId);
        Document savedDocument = documentRepository.save(document);

        DocumentVersion version = buildVersion(savedDocument, request, 1, actingUserId);
        DocumentVersion savedVersion = documentVersionRepository.save(version);

        savedDocument.setCurrentVersion(savedVersion);
        Document result = documentRepository.save(savedDocument);

        auditLogService.record(actingUserId, "DOCUMENT_UPLOAD", "Document", result.getId(), null, result, null, "API");
        return result;
    }

    @Override
    @Transactional
    public Document addVersion(UUID documentId, DocumentUploadRequest request, UUID actingUserId) {
        Document document = get(documentId);
        int nextVersion = documentVersionRepository.findByDocumentIdOrderByVersionNumberAsc(documentId).size() + 1;

        DocumentVersion version = buildVersion(document, request, nextVersion, actingUserId);
        DocumentVersion savedVersion = documentVersionRepository.save(version);

        document.setCurrentVersion(savedVersion);
        Document result = documentRepository.save(document);

        auditLogService.record(actingUserId, "DOCUMENT_NEW_VERSION", "Document", documentId,
                null, savedVersion, null, "API");
        return result;
    }

    @Override
    public List<Document> listByEntity(String relatedEntityType, UUID relatedEntityId) {
        return documentRepository.findByRelatedEntityTypeAndRelatedEntityId(relatedEntityType, relatedEntityId);
    }

    @Override
    public Document get(UUID id) {
        return documentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Document", id));
    }

    private DocumentVersion buildVersion(Document document, DocumentUploadRequest request, int versionNumber, UUID actingUserId) {
        DocumentVersion version = new DocumentVersion();
        version.setDocument(document);
        version.setVersionNumber(versionNumber);
        version.setFileName(request.fileName());
        version.setStoragePath(request.storagePath());
        version.setContentHash(request.contentHash());
        version.setUploadedBy(actingUserId);
        return version;
    }

    @Override
    @Transactional
    public Document uploadFile(MultipartFile file, DocumentCategory category,
                            String type, UUID entityId, UUID userId) {
        String ct = file.getContentType();
        if (ct == null || !List.of("application/pdf", "image/jpeg", "image/png").contains(ct))
            throw new IllegalArgumentException("Only PDF, JPG or PNG files are allowed");
        try {
            Path dir = Path.of(storageDir, type.toLowerCase(), entityId.toString());
            Files.createDirectories(dir);
            String original = Path.of(file.getOriginalFilename()).getFileName().toString();
            Path target = dir.resolve(UUID.randomUUID() + "_" + original);
            file.transferTo(target);
            String hash = HexFormat.of().formatHex(
                    MessageDigest.getInstance("SHA-256").digest(Files.readAllBytes(target)));
            return upload(new DocumentUploadRequest(category, type, entityId,
                    original, target.toString(), hash), userId);
        } catch (Exception e) {
            throw new IllegalStateException("File upload failed", e);
        }
    }

    @Override
    @Transactional
    public Document verify(UUID id, boolean approved, String note, UUID userId) {
        Document doc = get(id);
        DocumentVersion v = doc.getCurrentVersion();
        DocumentVerificationStatus before = v.getVerificationStatus();
        v.setVerificationStatus(approved ? DocumentVerificationStatus.VERIFIED
                                        : DocumentVerificationStatus.REJECTED);
        v.setVerifiedBy(userId);
        v.setVerifiedAt(Instant.now());
        v.setVerificationNote(note);
        documentVersionRepository.save(v);
        auditLogService.record(userId, approved ? "DOCUMENT_VERIFY" : "DOCUMENT_REJECT",
                "Document", id, before, v.getVerificationStatus(), note, "API");
        return doc;
    }
}
