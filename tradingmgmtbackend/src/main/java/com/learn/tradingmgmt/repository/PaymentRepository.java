package com.learn.tradingmgmt.repository;

import com.learn.tradingmgmt.entity.Payment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface PaymentRepository extends JpaRepository<Payment, UUID> {
    List<Payment> findByTransactionId(UUID transactionId);
    Optional<Payment> findByIdempotencyKey(String idempotencyKey);
}
