package com.learn.tradingmgmt.entity;

import com.learn.tradingmgmt.entity.enums.TransferStatus;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name = "share_transfers")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ShareTransfer extends BaseAuditableEntity {

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "transaction_id", nullable = false)
    private Transaction transaction;

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "buyer_customer_id", nullable = false)
    private Customer buyerCustomer;

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "seller_customer_id", nullable = false)
    private Customer sellerCustomer;

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "security_id", nullable = false)
    private ShareSecurity security;

    @Column(nullable = false, precision = 18, scale = 4)
    private BigDecimal quantity;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private TransferStatus status = TransferStatus.TRANSFER_PENDING;

    @Column(name = "initiated_date")
    private LocalDate initiatedDate;

    @Column(name = "submitted_date")
    private LocalDate submittedDate;

    @Column(name = "completed_date")
    private LocalDate completedDate;

    @Column(name = "rejection_reason", columnDefinition = "text")
    private String rejectionReason;

    @Column(name = "idempotency_key", unique = true)
    private String idempotencyKey;
}
