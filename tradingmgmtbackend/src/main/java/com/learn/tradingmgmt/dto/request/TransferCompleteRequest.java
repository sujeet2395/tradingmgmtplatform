package com.learn.tradingmgmt.dto.request;

import java.time.LocalDate;

public record TransferCompleteRequest(
        LocalDate completedDate
) {}
