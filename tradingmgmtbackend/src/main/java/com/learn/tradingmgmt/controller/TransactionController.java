package com.learn.tradingmgmt.controller;

import com.learn.tradingmgmt.dto.request.PaymentInitiateRequest;
import com.learn.tradingmgmt.dto.request.TransferInitiateRequest;
import com.learn.tradingmgmt.entity.Payment;
import com.learn.tradingmgmt.entity.ShareTransfer;
import com.learn.tradingmgmt.entity.Transaction;
import com.learn.tradingmgmt.service.PaymentService;
import com.learn.tradingmgmt.service.ShareTransferService;
import com.learn.tradingmgmt.service.TransactionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/transactions")
@RequiredArgsConstructor
public class TransactionController {

    private final TransactionService transactionService;
    private final PaymentService paymentService;
    private final ShareTransferService shareTransferService;

    @GetMapping("/{id}")
    public Transaction get(@PathVariable UUID id) {
        return transactionService.getTransaction(id);
    }

    @PostMapping("/{id}/payment")
    public ResponseEntity<Payment> initiatePayment(@PathVariable UUID id, @Valid @RequestBody PaymentInitiateRequest request,
                                                     @RequestHeader(value = "X-User-Id", required = false) UUID actingUserId) {
        return ResponseEntity.status(HttpStatus.CREATED).body(paymentService.initiatePayment(id, request, actingUserId));
    }

    @GetMapping("/{id}/payments")
    public List<Payment> listPayments(@PathVariable UUID id) {
        return paymentService.listByTransaction(id);
    }

    @PostMapping("/{id}/transfer")
    public ResponseEntity<ShareTransfer> initiateTransfer(@PathVariable UUID id, @RequestBody(required = false) TransferInitiateRequest request,
                                                             @RequestHeader(value = "X-User-Id", required = false) UUID actingUserId) {
        TransferInitiateRequest body = request != null ? request : new TransferInitiateRequest(null);
        return ResponseEntity.status(HttpStatus.CREATED).body(shareTransferService.initiateTransfer(id, body, actingUserId));
    }

    @GetMapping("/{id}/transfers")
    public List<ShareTransfer> listTransfers(@PathVariable UUID id) {
        return shareTransferService.listByTransaction(id);
    }

    @GetMapping("/by-order/{orderId}")
    public Transaction getByOrder(@PathVariable UUID orderId) {
        return transactionService.getByOrderId(orderId);
    }
}
