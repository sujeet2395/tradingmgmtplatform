package com.learn.tradingmgmt.repository;

import com.learn.tradingmgmt.entity.PaymentEvent;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface PaymentEventRepository extends JpaRepository<PaymentEvent, UUID> {
    List<PaymentEvent> findByPaymentIdOrderByOccurredAtAsc(UUID paymentId);
}
