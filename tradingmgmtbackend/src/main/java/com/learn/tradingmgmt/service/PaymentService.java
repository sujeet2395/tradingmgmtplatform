package com.learn.tradingmgmt.service;

import com.learn.tradingmgmt.dto.request.ChequeDepositRequest;
import com.learn.tradingmgmt.dto.request.ChequeReceivedRequest;
import com.learn.tradingmgmt.dto.request.PaymentInitiateRequest;
import com.learn.tradingmgmt.dto.request.PaymentVerifyRequest;
import com.learn.tradingmgmt.entity.Payment;

import java.util.List;
import java.util.UUID;

public interface PaymentService {
    Payment initiatePayment(UUID transactionId, PaymentInitiateRequest request, UUID actingUserId);
    Payment markChequeReceived(UUID paymentId, ChequeReceivedRequest request, UUID actingUserId);
    Payment markChequeDeposited(UUID paymentId, ChequeDepositRequest request, UUID actingUserId);
    /** Intended to be called only by users with role FINANCE (enforced once auth is added). */
    Payment verifyPayment(UUID paymentId, PaymentVerifyRequest request, UUID actingUserId);
    Payment get(UUID id);
    List<Payment> listByTransaction(UUID transactionId);
}
