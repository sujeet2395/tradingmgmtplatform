package com.learn.tradingmgmt.controller;

import com.learn.tradingmgmt.dto.request.ChequeDepositRequest;
import com.learn.tradingmgmt.dto.request.ChequeReceivedRequest;
import com.learn.tradingmgmt.dto.request.PaymentVerifyRequest;
import com.learn.tradingmgmt.entity.Payment;
import com.learn.tradingmgmt.service.PaymentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/payments")
@RequiredArgsConstructor
public class PaymentController {

    private final PaymentService paymentService;

    @GetMapping("/{id}")
    public Payment get(@PathVariable UUID id) {
        return paymentService.get(id);
    }

    @PostMapping("/{id}/cheque-received")
    public Payment chequeReceived(@PathVariable UUID id, @Valid @RequestBody ChequeReceivedRequest request,
                                   @RequestHeader(value = "X-User-Id", required = false) UUID actingUserId) {
        return paymentService.markChequeReceived(id, request, actingUserId);
    }

    @PostMapping("/{id}/deposit")
    public Payment deposit(@PathVariable UUID id, @RequestBody(required = false) ChequeDepositRequest request,
                            @RequestHeader(value = "X-User-Id", required = false) UUID actingUserId) {
        ChequeDepositRequest body = request != null ? request : new ChequeDepositRequest(null);
        return paymentService.markChequeDeposited(id, body, actingUserId);
    }

    @PostMapping("/{id}/verify")
    public Payment verify(@PathVariable UUID id, @RequestBody(required = false) PaymentVerifyRequest request,
                           @RequestHeader(value = "X-User-Id", required = false) UUID actingUserId) {
        PaymentVerifyRequest body = request != null ? request : new PaymentVerifyRequest(null, null);
        return paymentService.verifyPayment(id, body, actingUserId);
    }
}
