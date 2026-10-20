package com.learn.tradingmgmt.entity;

import com.learn.tradingmgmt.entity.enums.PaymentEventType;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.UuidGenerator;

import java.time.Instant;
import java.util.UUID;

/**
 * Append-only history behind Payment.status. Application code should never
 * UPDATE payments.status directly — insert a PaymentEvent and derive the
 * new status in the same service-layer transaction instead.
 */
@Entity
@Table(name = "payment_events")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class PaymentEvent {

    @Id
    @GeneratedValue
    @UuidGenerator
    private UUID id;

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "payment_id", nullable = false)
    private Payment payment;

    @Enumerated(EnumType.STRING)
    @Column(name = "event_type", nullable = false, length = 30)
    private PaymentEventType eventType;

    @Column(name = "event_data", columnDefinition = "text")
    private String eventData;

    @Column(name = "occurred_at", nullable = false)
    private Instant occurredAt;

    @Column(name = "created_by")
    private UUID createdBy;

    @PrePersist
    void onCreate() {
        this.occurredAt = Instant.now();
    }
}
