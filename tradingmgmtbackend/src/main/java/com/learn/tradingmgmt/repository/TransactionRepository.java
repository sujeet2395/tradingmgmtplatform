package com.learn.tradingmgmt.repository;

import com.learn.tradingmgmt.entity.Transaction;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface TransactionRepository extends JpaRepository<Transaction, UUID> {
    Optional<Transaction> findByOrderId(UUID orderId);
    List<Transaction> findByStatus(com.learn.tradingmgmt.entity.enums.TransactionStatus status);
}
