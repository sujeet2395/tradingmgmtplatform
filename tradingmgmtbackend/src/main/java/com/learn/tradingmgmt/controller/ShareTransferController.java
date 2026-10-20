package com.learn.tradingmgmt.controller;

import com.learn.tradingmgmt.dto.request.TransferCompleteRequest;
import com.learn.tradingmgmt.dto.request.TransferRejectRequest;
import com.learn.tradingmgmt.dto.request.TransferSubmitRequest;
import com.learn.tradingmgmt.entity.ShareTransfer;
import com.learn.tradingmgmt.service.ShareTransferService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/transfers")
@RequiredArgsConstructor
public class ShareTransferController {

    private final ShareTransferService shareTransferService;

    @GetMapping("/{id}")
    public ShareTransfer get(@PathVariable UUID id) {
        return shareTransferService.get(id);
    }

    @PostMapping("/{id}/submit")
    public ShareTransfer submit(@PathVariable UUID id, @RequestBody(required = false) TransferSubmitRequest request,
                                 @RequestHeader(value = "X-User-Id", required = false) UUID actingUserId) {
        TransferSubmitRequest body = request != null ? request : new TransferSubmitRequest(null);
        return shareTransferService.submitTransfer(id, body, actingUserId);
    }

    @PostMapping("/{id}/complete")
    public ShareTransfer complete(@PathVariable UUID id, @RequestBody(required = false) TransferCompleteRequest request,
                                   @RequestHeader(value = "X-User-Id", required = false) UUID actingUserId) {
        TransferCompleteRequest body = request != null ? request : new TransferCompleteRequest(null);
        return shareTransferService.completeTransfer(id, body, actingUserId);
    }

    @PostMapping("/{id}/reject")
    public ShareTransfer reject(@PathVariable UUID id, @Valid @RequestBody TransferRejectRequest request,
                                 @RequestHeader(value = "X-User-Id", required = false) UUID actingUserId) {
        return shareTransferService.rejectTransfer(id, request, actingUserId);
    }
}
