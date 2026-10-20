package com.learn.tradingmgmt.service;

import com.learn.tradingmgmt.dto.request.DocumentUploadRequest;
import com.learn.tradingmgmt.entity.Document;
import com.learn.tradingmgmt.entity.enums.DocumentCategory;

import java.util.List;
import java.util.UUID;

import org.springframework.web.multipart.MultipartFile;

public interface DocumentService {
    Document upload(DocumentUploadRequest request, UUID actingUserId);
    Document addVersion(UUID documentId, DocumentUploadRequest request, UUID actingUserId);
    List<Document> listByEntity(String relatedEntityType, UUID relatedEntityId);
    Document get(UUID id);
    Document uploadFile(MultipartFile file, DocumentCategory category,
                    String relatedEntityType, UUID relatedEntityId, UUID actingUserId);
    Document verify(UUID documentId, boolean approved, String note, UUID actingUserId);
}
