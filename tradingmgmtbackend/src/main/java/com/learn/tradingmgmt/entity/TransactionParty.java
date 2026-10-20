package com.learn.tradingmgmt.entity;

import com.learn.tradingmgmt.entity.enums.TransactionPartyRole;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.UuidGenerator;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "transaction_parties",
       uniqueConstraints = @UniqueConstraint(columnNames = {"transaction_id", "customer_id", "role"}))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class TransactionParty {

    @Id
    @GeneratedValue
    @UuidGenerator
    private UUID id;

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "transaction_id", nullable = false)
    private Transaction transaction;

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "customer_id", nullable = false)
    private Customer customer;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private TransactionPartyRole role;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @PrePersist
    void onCreate() {
        this.createdAt = Instant.now();
    }
}
