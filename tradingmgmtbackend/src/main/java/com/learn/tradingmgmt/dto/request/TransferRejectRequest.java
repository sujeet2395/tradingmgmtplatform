package com.learn.tradingmgmt.dto.request;

import jakarta.validation.constraints.NotBlank;

public record TransferRejectRequest(
        @NotBlank String reason
) {}
