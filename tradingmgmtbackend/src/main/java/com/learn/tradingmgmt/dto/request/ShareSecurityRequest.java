package com.learn.tradingmgmt.dto.request;

import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.util.UUID;

public record ShareSecurityRequest(
        @NotNull UUID companyId,
        String securityType,
        String isin,
        BigDecimal faceValue,
        String shareClass,
        BigDecimal referencePrice
) {}
