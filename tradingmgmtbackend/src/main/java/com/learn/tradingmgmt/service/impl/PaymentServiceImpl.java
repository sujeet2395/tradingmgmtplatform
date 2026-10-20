package com.learn.tradingmgmt.service.impl;

import com.learn.tradingmgmt.dto.request.ChequeDepositRequest;
import com.learn.tradingmgmt.dto.request.ChequeReceivedRequest;
import com.learn.tradingmgmt.dto.request.PaymentInitiateRequest;
import com.learn.tradingmgmt.dto.request.PaymentVerifyRequest;
import com.learn.tradingmgmt.entity.Payment;
import com.learn.tradingmgmt.entity.PaymentEvent;
import com.learn.tradingmgmt.entity.Transaction;
import com.learn.tradingmgmt.entity.enums.PaymentEventType;
import com.learn.tradingmgmt.entity.enums.PaymentStatus;
import com.learn.tradingmgmt.entity.enums.TransactionStatus;
import com.learn.tradingmgmt.exception.InvalidStateTransitionException;
import com.learn.tradingmgmt.exception.ResourceNotFoundException;
import com.learn.tradingmgmt.repository.PaymentEventRepository;
import com.learn.tradingmgmt.repository.PaymentRepository;
import com.learn.tradingmgmt.service.AuditLogService;
import com.learn.tradingmgmt.service.PaymentService;
import com.learn.tradingmgmt.service.TransactionService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * Every state-changing method here: (1) validates the current Payment
 * status, (2) updates the Payment row, (3) inserts an append-only
 * PaymentEvent, (4) pushes the parent Transaction forward via
 * TransactionService.transition, (5) writes an AuditLog entry. Never
 * skip any of these four when adding a new command.
 */
@Service
@RequiredArgsConstructor
public class PaymentServiceImpl implements PaymentService {

    private final PaymentRepository paymentRepository;
    private final PaymentEventRepository paymentEventRepository;
    private final TransactionService transactionService;
    private final AuditLogService auditLogService;

    @Override
    @Transactional
    public Payment initiatePayment(UUID transactionId, PaymentInitiateRequest request, UUID actingUserId) {
        Transaction transaction = transactionService.getTransaction(transactionId);
        requireTransactionStatus(transaction, Set.of(TransactionStatus.ORDER_CONFIRMED, TransactionStatus.PAYMENT_FAILED));

        Payment payment = new Payment();
        payment.setTransaction(transaction);
        payment.setAmount(request.amount());
        if (request.paymentMethod() != null) payment.setPaymentMethod(request.paymentMethod());
        payment.setIdempotencyKey(request.idempotencyKey());
        payment.setCreatedBy(actingUserId);
        payment.setUpdatedBy(actingUserId);
        Payment saved = paymentRepository.save(payment);

        transactionService.transition(transactionId, TransactionStatus.PAYMENT_PENDING, actingUserId,
                "Payment " + saved.getId() + " initiated");
        auditLogService.record(actingUserId, "PAYMENT_INITIATE", "Payment", saved.getId(), null, saved, null, "API");
        return saved;
    }

    @Override
    @Transactional
    public Payment markChequeReceived(UUID paymentId, ChequeReceivedRequest request, UUID actingUserId) {
        Payment payment = get(paymentId);
        requirePaymentStatus(payment, PaymentStatus.PAYMENT_PENDING);

        payment.setChequeNumber(request.chequeNumber());
        payment.setBankName(request.bankName());
        payment.setChequeDate(request.chequeDate() != null ? request.chequeDate() : LocalDate.now());
        payment.setStatus(PaymentStatus.CHEQUE_RECEIVED);
        payment.setUpdatedBy(actingUserId);
        Payment saved = paymentRepository.save(payment);

        recordEvent(saved, PaymentEventType.CHEQUE_RECEIVED, actingUserId);
        auditLogService.record(actingUserId, "PAYMENT_CHEQUE_RECEIVED", "Payment", paymentId,
                PaymentStatus.PAYMENT_PENDING, PaymentStatus.CHEQUE_RECEIVED, null, "API");
        return saved;
    }

    @Override
    @Transactional
    public Payment markChequeDeposited(UUID paymentId, ChequeDepositRequest request, UUID actingUserId) {
        Payment payment = get(paymentId);
        requirePaymentStatus(payment, PaymentStatus.CHEQUE_RECEIVED);

        payment.setDepositDate(request.depositDate() != null ? request.depositDate() : LocalDate.now());
        payment.setStatus(PaymentStatus.CHEQUE_DEPOSITED);
        payment.setUpdatedBy(actingUserId);
        Payment saved = paymentRepository.save(payment);

        recordEvent(saved, PaymentEventType.CHEQUE_DEPOSITED, actingUserId);
        transactionService.transition(payment.getTransaction().getId(), TransactionStatus.PAYMENT_RECEIVED,
                actingUserId, "Cheque deposited for payment " + paymentId);
        auditLogService.record(actingUserId, "PAYMENT_CHEQUE_DEPOSITED", "Payment", paymentId,
                PaymentStatus.CHEQUE_RECEIVED, PaymentStatus.CHEQUE_DEPOSITED, null, "API");
        return saved;
    }

    @Override
    @Transactional
    public Payment verifyPayment(UUID paymentId, PaymentVerifyRequest request, UUID actingUserId) {
        // TODO: once Spring Security is wired in, restrict this call to role FINANCE.
        Payment payment = get(paymentId);
        requirePaymentStatus(payment, Set.of(PaymentStatus.CHEQUE_DEPOSITED, PaymentStatus.CHEQUE_CLEARED));

        payment.setConfirmationDate(request.confirmationDate() != null ? request.confirmationDate() : LocalDate.now());
        payment.setStatus(PaymentStatus.PAYMENT_CONFIRMED);
        payment.setUpdatedBy(actingUserId);
        Payment saved = paymentRepository.save(payment);

        recordEvent(saved, PaymentEventType.PAYMENT_VERIFIED, actingUserId);
        transactionService.transition(payment.getTransaction().getId(), TransactionStatus.PAYMENT_VERIFIED,
                actingUserId, request.reason());
        auditLogService.record(actingUserId, "PAYMENT_VERIFY", "Payment", paymentId,
                PaymentStatus.CHEQUE_DEPOSITED, PaymentStatus.PAYMENT_CONFIRMED, request.reason(), "API");
        return saved;
    }

    @Override
    public Payment get(UUID id) {
        return paymentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Payment", id));
    }

    @Override
    public List<Payment> listByTransaction(UUID transactionId) {
        return paymentRepository.findByTransactionId(transactionId);
    }

    private void recordEvent(Payment payment, PaymentEventType type, UUID actingUserId) {
        PaymentEvent event = new PaymentEvent();
        event.setPayment(payment);
        event.setEventType(type);
        event.setCreatedBy(actingUserId);
        paymentEventRepository.save(event);
    }

    private void requirePaymentStatus(Payment payment, PaymentStatus expected) {
        requirePaymentStatus(payment, Set.of(expected));
    }

    private void requirePaymentStatus(Payment payment, Set<PaymentStatus> expectedAny) {
        if (!expectedAny.contains(payment.getStatus())) {
            throw new InvalidStateTransitionException(
                    "Payment " + payment.getId() + " is " + payment.getStatus() + ", expected one of " + expectedAny);
        }
    }

    private void requireTransactionStatus(Transaction transaction, Set<TransactionStatus> expectedAny) {
        if (!expectedAny.contains(transaction.getStatus())) {
            throw new InvalidStateTransitionException(
                    "Transaction " + transaction.getId() + " is " + transaction.getStatus() + ", expected one of " + expectedAny);
        }
    }
}
