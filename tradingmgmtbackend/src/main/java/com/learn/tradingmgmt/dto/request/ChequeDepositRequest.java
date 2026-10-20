package com.learn.tradingmgmt.dto.request;

import java.time.LocalDate;

public record ChequeDepositRequest(
        LocalDate depositDate
) {}
