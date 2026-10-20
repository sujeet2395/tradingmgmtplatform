package com.learn.tradingmgmt.dto.request;

import com.learn.tradingmgmt.entity.enums.PaymentMethod;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public record PaymentInitiateRequest(
        @NotNull @DecimalMin(value = "0.0001") BigDecimal amount,
        PaymentMethod paymentMethod,
        String idempotencyKey
) {}
