package com.learn.tradingmgmt.service.impl;

import com.learn.tradingmgmt.entity.Transaction;
import com.learn.tradingmgmt.entity.enums.TransactionStatus;
import com.learn.tradingmgmt.exception.InvalidStateTransitionException;
import com.learn.tradingmgmt.exception.ResourceNotFoundException;
import com.learn.tradingmgmt.repository.TransactionRepository;
import com.learn.tradingmgmt.service.AuditLogService;
import com.learn.tradingmgmt.service.TransactionService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.EnumMap;
import java.util.EnumSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import static com.learn.tradingmgmt.entity.enums.TransactionStatus.*;

@Service
@RequiredArgsConstructor
public class TransactionServiceImpl implements TransactionService {

    private final TransactionRepository transactionRepository;
    private final AuditLogService auditLogService;

    /**
     * Single source of truth for which transitions are legal. Mirrors the
     * state machine in the business plan (section 6.1). Any transition not
     * listed here is rejected.
     */
    private static final Map<TransactionStatus, Set<TransactionStatus>> ALLOWED = new EnumMap<>(TransactionStatus.class);
    static {
        ALLOWED.put(DRAFT, EnumSet.of(ORDER_CONFIRMED, CANCELLED));
        ALLOWED.put(ORDER_CONFIRMED, EnumSet.of(PAYMENT_PENDING, CANCELLED));
        ALLOWED.put(PAYMENT_PENDING, EnumSet.of(PAYMENT_RECEIVED, PAYMENT_FAILED, CANCELLED));
        ALLOWED.put(PAYMENT_RECEIVED, EnumSet.of(PAYMENT_VERIFIED, PAYMENT_FAILED));
        ALLOWED.put(PAYMENT_VERIFIED, EnumSet.of(TRANSFER_PENDING));
        ALLOWED.put(PAYMENT_FAILED, EnumSet.of(PAYMENT_PENDING, CANCELLED));
        ALLOWED.put(TRANSFER_PENDING, EnumSet.of(TRANSFER_INITIATED, TRANSFER_REJECTED));
        ALLOWED.put(TRANSFER_INITIATED, EnumSet.of(TRANSFER_COMPLETED, TRANSFER_REJECTED));
        ALLOWED.put(TRANSFER_COMPLETED, EnumSet.of(SETTLED));
        ALLOWED.put(TRANSFER_REJECTED, EnumSet.of(TRANSFER_PENDING, CANCELLED));
    }

    @Override
    public Transaction getTransaction(UUID id) {
        return transactionRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Transaction", id));
    }

    @Override
    @Transactional
    public Transaction transition(UUID transactionId, TransactionStatus newStatus, UUID actingUserId, String reason) {
        Transaction transaction = getTransaction(transactionId);
        TransactionStatus current = transaction.getStatus();

        Set<TransactionStatus> allowedNext = ALLOWED.getOrDefault(current, Set.of());
        if (!allowedNext.contains(newStatus)) {
            throw new InvalidStateTransitionException(
                    "Cannot move transaction " + transactionId + " from " + current + " to " + newStatus);
        }

        transaction.setStatus(newStatus);
        if (newStatus == SETTLED) {
            transaction.setSettledAt(java.time.Instant.now());
        }
        Transaction saved = transactionRepository.save(transaction);

        auditLogService.record(actingUserId, "TRANSACTION_STATUS_CHANGE", "Transaction", transactionId,
                current, newStatus, reason, "API");
        return saved;
    }

    @Override
    public Transaction getByOrderId(UUID orderId) {
        return transactionRepository.findByOrderId(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Transaction for order", orderId));
    }
}
