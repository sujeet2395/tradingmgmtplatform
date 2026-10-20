package com.learn.tradingmgmt.dto.request;

import jakarta.validation.constraints.NotBlank;

import java.time.LocalDate;

public record ChequeReceivedRequest(
        @NotBlank String chequeNumber,
        String bankName,
        LocalDate chequeDate
) {}
