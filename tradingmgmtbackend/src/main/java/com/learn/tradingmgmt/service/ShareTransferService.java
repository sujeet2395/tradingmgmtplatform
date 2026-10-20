package com.learn.tradingmgmt.service;

import com.learn.tradingmgmt.dto.request.TransferCompleteRequest;
import com.learn.tradingmgmt.dto.request.TransferInitiateRequest;
import com.learn.tradingmgmt.dto.request.TransferRejectRequest;
import com.learn.tradingmgmt.dto.request.TransferSubmitRequest;
import com.learn.tradingmgmt.entity.ShareTransfer;

import java.util.List;
import java.util.UUID;

public interface ShareTransferService {
    /** Requires the parent transaction to be in PAYMENT_VERIFIED status. */
    ShareTransfer initiateTransfer(UUID transactionId, TransferInitiateRequest request, UUID actingUserId);
    ShareTransfer submitTransfer(UUID transferId, TransferSubmitRequest request, UUID actingUserId);
    /** Marks the transfer complete and settles the parent transaction. */
    ShareTransfer completeTransfer(UUID transferId, TransferCompleteRequest request, UUID actingUserId);
    ShareTransfer rejectTransfer(UUID transferId, TransferRejectRequest request, UUID actingUserId);
    ShareTransfer get(UUID id);
    List<ShareTransfer> listByTransaction(UUID transactionId);
}
