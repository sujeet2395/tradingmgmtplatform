package com.learn.tradingmgmt.service;

import com.learn.tradingmgmt.entity.Transaction;
import com.learn.tradingmgmt.entity.enums.TransactionStatus;

import java.util.UUID;

public interface TransactionService {
    Transaction getTransaction(UUID id);

    /**
     * Validates the transition against the allowed state map and persists
     * it. Called internally by PaymentService / ShareTransferService — not
     * exposed as a generic "set status" endpoint on purpose (see plan
     * rule: no direct status edits).
     */
    Transaction transition(UUID transactionId, TransactionStatus newStatus, UUID actingUserId, String reason);

    Transaction getByOrderId(UUID orderId);
}
