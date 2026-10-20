package com.learn.tradingmgmt.dto.request;

import jakarta.validation.constraints.NotBlank;

public record CompanyRequest(
        @NotBlank String legalName,
        String cin,
        String status
) {}
