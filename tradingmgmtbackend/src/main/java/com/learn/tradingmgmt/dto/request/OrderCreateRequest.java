package com.learn.tradingmgmt.dto.request;

import com.learn.tradingmgmt.entity.enums.OrderType;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

public record OrderCreateRequest(
        @NotNull OrderType orderType,
        UUID buyerCustomerId,
        UUID sellerCustomerId,
        @NotNull UUID securityId,
        @NotNull @DecimalMin(value = "0.0001") BigDecimal quantity,
        @NotNull @DecimalMin(value = "0.0001") BigDecimal pricePerShare,
        LocalDate orderDate,
        String notes
) {}
