package com.learn.tradingmgmt.dto.request;

import java.time.LocalDate;

public record PaymentVerifyRequest(
        LocalDate confirmationDate,
        String reason
) {}
