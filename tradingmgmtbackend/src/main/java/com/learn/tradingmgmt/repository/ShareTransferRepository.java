package com.learn.tradingmgmt.repository;

import com.learn.tradingmgmt.entity.ShareTransfer;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ShareTransferRepository extends JpaRepository<ShareTransfer, UUID> {
    List<ShareTransfer> findByTransactionId(UUID transactionId);
    Optional<ShareTransfer> findByIdempotencyKey(String idempotencyKey);
}
