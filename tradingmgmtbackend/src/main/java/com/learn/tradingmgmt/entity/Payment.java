package com.learn.tradingmgmt.entity;

import com.learn.tradingmgmt.entity.enums.PaymentMethod;
import com.learn.tradingmgmt.entity.enums.PaymentStatus;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name = "payments")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Payment extends BaseAuditableEntity {

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "transaction_id", nullable = false)
    private Transaction transaction;

    @Column(nullable = false, precision = 18, scale = 4)
    private BigDecimal amount;

    @Enumerated(EnumType.STRING)
    @Column(name = "payment_method", nullable = false, length = 20)
    private PaymentMethod paymentMethod = PaymentMethod.CHEQUE;

    @Column(name = "cheque_number")
    private String chequeNumber;

    @Column(name = "bank_name")
    private String bankName;

    @Column(name = "cheque_date")
    private LocalDate chequeDate;

    @Column(name = "deposit_date")
    private LocalDate depositDate;

    @Column(name = "clearance_date")
    private LocalDate clearanceDate;

    @Column(name = "confirmation_date")
    private LocalDate confirmationDate;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private PaymentStatus status = PaymentStatus.PAYMENT_PENDING;

    @Column(name = "idempotency_key", unique = true)
    private String idempotencyKey;
}
