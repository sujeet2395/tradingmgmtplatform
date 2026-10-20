package com.learn.tradingmgmt.dto.request;

import com.learn.tradingmgmt.entity.enums.DocumentCategory;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record DocumentUploadRequest(
        @NotNull DocumentCategory category,
        @NotBlank String relatedEntityType,
        @NotNull UUID relatedEntityId,
        @NotBlank String fileName,
        @NotBlank String storagePath,
        String contentHash
) {}
