package com.learn.tradingmgmt.repository;

import com.learn.tradingmgmt.entity.TransferEvent;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface TransferEventRepository extends JpaRepository<TransferEvent, UUID> {
    List<TransferEvent> findByTransferIdOrderByOccurredAtAsc(UUID transferId);
}
