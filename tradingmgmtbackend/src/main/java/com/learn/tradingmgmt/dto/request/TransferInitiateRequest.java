package com.learn.tradingmgmt.dto.request;

/** Buyer/seller/security/quantity are taken from the parent Transaction;
 *  this request only carries fields the operator can override at
 *  initiation time. */
public record TransferInitiateRequest(
        String idempotencyKey
) {}
