package com.learn.tradingmgmt.repository;

import com.learn.tradingmgmt.entity.TransactionParty;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface TransactionPartyRepository extends JpaRepository<TransactionParty, UUID> {
    List<TransactionParty> findByTransactionId(UUID transactionId);
}
