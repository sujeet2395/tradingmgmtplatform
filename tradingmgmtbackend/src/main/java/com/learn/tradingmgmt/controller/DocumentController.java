package com.learn.tradingmgmt.controller;

import com.learn.tradingmgmt.dto.request.DocumentUploadRequest;
import com.learn.tradingmgmt.entity.Document;
import com.learn.tradingmgmt.entity.DocumentVersion;
import com.learn.tradingmgmt.entity.enums.DocumentCategory;
import com.learn.tradingmgmt.service.DocumentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/documents")
@RequiredArgsConstructor
public class DocumentController {

    private final DocumentService documentService;

    @PostMapping
    public ResponseEntity<Document> upload(@Valid @RequestBody DocumentUploadRequest request,
                                            @RequestHeader(value = "X-User-Id", required = false) UUID actingUserId) {
        return ResponseEntity.status(HttpStatus.CREATED).body(documentService.upload(request, actingUserId));
    }

    @PostMapping("/{id}/versions")
    public Document addVersion(@PathVariable UUID id, @Valid @RequestBody DocumentUploadRequest request,
                                @RequestHeader(value = "X-User-Id", required = false) UUID actingUserId) {
        return documentService.addVersion(id, request, actingUserId);
    }

    @GetMapping("/{id}")
    public Document get(@PathVariable UUID id) {
        return documentService.get(id);
    }

    @GetMapping
    public List<Document> listByEntity(@RequestParam String relatedEntityType, @RequestParam UUID relatedEntityId) {
        return documentService.listByEntity(relatedEntityType, relatedEntityId);
    }

    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<Document> uploadFile(@RequestParam MultipartFile file,
            @RequestParam DocumentCategory category,
            @RequestParam String relatedEntityType, @RequestParam UUID relatedEntityId,
            @RequestHeader(value = "X-User-Id", required = false) UUID actingUserId) {
        return ResponseEntity.status(HttpStatus.CREATED).body(
            documentService.uploadFile(file, category, relatedEntityType, relatedEntityId, actingUserId));
    }

    @PostMapping("/{id}/verify")
    public Document verify(@PathVariable UUID id, @RequestBody(required = false) Map<String, String> body,
            @RequestHeader(value = "X-User-Id", required = false) UUID actingUserId) {
        return documentService.verify(id, true, body == null ? null : body.get("note"), actingUserId);
    }

    @PostMapping("/{id}/reject")
    public Document reject(@PathVariable UUID id, @RequestBody Map<String, String> body,
            @RequestHeader(value = "X-User-Id", required = false) UUID actingUserId) {
        return documentService.verify(id, false, body.get("note"), actingUserId);
    }

    @GetMapping("/{id}/download")
    public ResponseEntity<Resource> download(@PathVariable UUID id) {
        DocumentVersion v = documentService.get(id).getCurrentVersion();
        return ResponseEntity.ok()
            .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + v.getFileName() + "\"")
            .body(new FileSystemResource(v.getStoragePath()));
    }
}
