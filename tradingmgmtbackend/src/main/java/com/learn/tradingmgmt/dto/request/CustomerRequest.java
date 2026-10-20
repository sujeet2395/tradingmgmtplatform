package com.learn.tradingmgmt.dto.request;

import jakarta.validation.constraints.NotBlank;

public record CustomerRequest(
        @NotBlank String fullName,
        String phone,
        String email,
        String address,
        String bankAccountName,
        String bankIfsc
) {}
