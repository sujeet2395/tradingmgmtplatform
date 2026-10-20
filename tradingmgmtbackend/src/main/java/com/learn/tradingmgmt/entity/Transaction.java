package com.learn.tradingmgmt.entity;

import com.learn.tradingmgmt.entity.enums.TransactionStatus;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * The actual settlement record for a confirmed Order. Kept separate from
 * Order so a single order can (in principle) settle across more than one
 * transaction later (e.g. partial fills) without a schema change.
 */
@Entity
@Table(name = "transactions")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Transaction extends BaseAuditableEntity {

    @Column(name = "transaction_number", nullable = false, unique = true)
    private String transactionNumber;

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "order_id", nullable = false)
    private Order order;

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "security_id", nullable = false)
    private ShareSecurity security;

    @Column(nullable = false, precision = 18, scale = 4)
    private BigDecimal quantity;

    @Column(name = "agreed_price", nullable = false, precision = 18, scale = 4)
    private BigDecimal agreedPrice;

    @Column(name = "total_amount", nullable = false, precision = 18, scale = 4)
    private BigDecimal totalAmount;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private TransactionStatus status = TransactionStatus.DRAFT;

    @Column(name = "settled_at")
    private Instant settledAt;
}
