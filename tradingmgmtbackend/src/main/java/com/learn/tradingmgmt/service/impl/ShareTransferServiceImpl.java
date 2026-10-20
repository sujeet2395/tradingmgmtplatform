package com.learn.tradingmgmt.service.impl;

import com.learn.tradingmgmt.dto.request.TransferCompleteRequest;
import com.learn.tradingmgmt.dto.request.TransferInitiateRequest;
import com.learn.tradingmgmt.dto.request.TransferRejectRequest;
import com.learn.tradingmgmt.dto.request.TransferSubmitRequest;
import com.learn.tradingmgmt.entity.Order;
import com.learn.tradingmgmt.entity.ShareTransfer;
import com.learn.tradingmgmt.entity.Transaction;
import com.learn.tradingmgmt.entity.TransferEvent;
import com.learn.tradingmgmt.entity.enums.TransactionStatus;
import com.learn.tradingmgmt.entity.enums.TransferEventType;
import com.learn.tradingmgmt.entity.enums.TransferStatus;
import com.learn.tradingmgmt.exception.InvalidStateTransitionException;
import com.learn.tradingmgmt.exception.ResourceNotFoundException;
import com.learn.tradingmgmt.repository.ShareTransferRepository;
import com.learn.tradingmgmt.repository.TransferEventRepository;
import com.learn.tradingmgmt.service.AuditLogService;
import com.learn.tradingmgmt.service.ShareTransferService;
import com.learn.tradingmgmt.service.TransactionService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ShareTransferServiceImpl implements ShareTransferService {

    private final ShareTransferRepository shareTransferRepository;
    private final TransferEventRepository transferEventRepository;
    private final TransactionService transactionService;
    private final AuditLogService auditLogService;

    @Override
    @Transactional
    public ShareTransfer initiateTransfer(UUID transactionId, TransferInitiateRequest request, UUID actingUserId) {
        Transaction transaction = transactionService.getTransaction(transactionId);
        requireTransactionStatus(transaction, TransactionStatus.PAYMENT_VERIFIED);

        Order order = transaction.getOrder();
        if (order.getBuyerCustomer() == null || order.getSellerCustomer() == null) {
            throw new InvalidStateTransitionException(
                    "Order " + order.getId() + " is missing buyer/seller — cannot initiate a share transfer");
        }

        ShareTransfer transfer = new ShareTransfer();
        transfer.setTransaction(transaction);
        transfer.setBuyerCustomer(order.getBuyerCustomer());
        transfer.setSellerCustomer(order.getSellerCustomer());
        transfer.setSecurity(transaction.getSecurity());
        transfer.setQuantity(transaction.getQuantity());
        transfer.setInitiatedDate(LocalDate.now());
        transfer.setIdempotencyKey(request.idempotencyKey());
        transfer.setCreatedBy(actingUserId);
        transfer.setUpdatedBy(actingUserId);
        ShareTransfer saved = shareTransferRepository.save(transfer);

        transactionService.transition(transactionId, TransactionStatus.TRANSFER_PENDING, actingUserId,
                "Share transfer " + saved.getId() + " initiated");
        auditLogService.record(actingUserId, "TRANSFER_INITIATE", "ShareTransfer", saved.getId(), null, saved, null, "API");
        return saved;
    }

    @Override
    @Transactional
    public ShareTransfer submitTransfer(UUID transferId, TransferSubmitRequest request, UUID actingUserId) {
        ShareTransfer transfer = get(transferId);
        requireTransferStatus(transfer, Set.of(TransferStatus.TRANSFER_PENDING, TransferStatus.DOCUMENTS_VERIFIED));

        transfer.setStatus(TransferStatus.TRANSFER_SUBMITTED);
        transfer.setSubmittedDate(LocalDate.now());
        transfer.setUpdatedBy(actingUserId);
        ShareTransfer saved = shareTransferRepository.save(transfer);

        recordEvent(saved, TransferEventType.TRANSFER_SUBMITTED, actingUserId);
        transactionService.transition(transfer.getTransaction().getId(), TransactionStatus.TRANSFER_INITIATED,
                actingUserId, request.note());
        auditLogService.record(actingUserId, "TRANSFER_SUBMIT", "ShareTransfer", transferId,
                TransferStatus.TRANSFER_PENDING, TransferStatus.TRANSFER_SUBMITTED, request.note(), "API");
        return saved;
    }

    @Override
    @Transactional
    public ShareTransfer completeTransfer(UUID transferId, TransferCompleteRequest request, UUID actingUserId) {
        ShareTransfer transfer = get(transferId);
        requireTransferStatus(transfer, Set.of(TransferStatus.TRANSFER_SUBMITTED, TransferStatus.TRANSFER_IN_PROCESS));

        transfer.setStatus(TransferStatus.TRANSFER_COMPLETED);
        transfer.setCompletedDate(request.completedDate() != null ? request.completedDate() : LocalDate.now());
        transfer.setUpdatedBy(actingUserId);
        ShareTransfer saved = shareTransferRepository.save(transfer);

        recordEvent(saved, TransferEventType.TRANSFER_COMPLETED, actingUserId);

        UUID transactionId = transfer.getTransaction().getId();
        transactionService.transition(transactionId, TransactionStatus.TRANSFER_COMPLETED, actingUserId,
                "Share transfer " + transferId + " completed");
        transactionService.transition(transactionId, TransactionStatus.SETTLED, actingUserId,
                "Payment verified and transfer completed");

        auditLogService.record(actingUserId, "TRANSFER_COMPLETE", "ShareTransfer", transferId,
                TransferStatus.TRANSFER_SUBMITTED, TransferStatus.TRANSFER_COMPLETED, null, "API");
        return saved;
    }

    @Override
    @Transactional
    public ShareTransfer rejectTransfer(UUID transferId, TransferRejectRequest request, UUID actingUserId) {
        ShareTransfer transfer = get(transferId);
        TransferStatus before = transfer.getStatus();

        transfer.setStatus(TransferStatus.TRANSFER_REJECTED);
        transfer.setRejectionReason(request.reason());
        transfer.setUpdatedBy(actingUserId);
        ShareTransfer saved = shareTransferRepository.save(transfer);

        recordEvent(saved, TransferEventType.TRANSFER_REJECTED, actingUserId);
        transactionService.transition(transfer.getTransaction().getId(), TransactionStatus.TRANSFER_REJECTED,
                actingUserId, request.reason());
        auditLogService.record(actingUserId, "TRANSFER_REJECT", "ShareTransfer", transferId,
                before, TransferStatus.TRANSFER_REJECTED, request.reason(), "API");
        return saved;
    }

    @Override
    public ShareTransfer get(UUID id) {
        return shareTransferRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("ShareTransfer", id));
    }

    @Override
    public List<ShareTransfer> listByTransaction(UUID transactionId) {
        return shareTransferRepository.findByTransactionId(transactionId);
    }

    private void recordEvent(ShareTransfer transfer, TransferEventType type, UUID actingUserId) {
        TransferEvent event = new TransferEvent();
        event.setTransfer(transfer);
        event.setEventType(type);
        event.setCreatedBy(actingUserId);
        transferEventRepository.save(event);
    }

    private void requireTransactionStatus(Transaction transaction, TransactionStatus expected) {
        if (transaction.getStatus() != expected) {
            throw new InvalidStateTransitionException(
                    "Transaction " + transaction.getId() + " is " + transaction.getStatus() + ", expected " + expected);
        }
    }

    private void requireTransferStatus(ShareTransfer transfer, Set<TransferStatus> expectedAny) {
        if (!expectedAny.contains(transfer.getStatus())) {
            throw new InvalidStateTransitionException(
                    "ShareTransfer " + transfer.getId() + " is " + transfer.getStatus() + ", expected one of " + expectedAny);
        }
    }
}
