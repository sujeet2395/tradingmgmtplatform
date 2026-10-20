package com.learn.tradingmgmt.entity;

import com.learn.tradingmgmt.entity.enums.TransferEventType;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.UuidGenerator;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "transfer_events")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class TransferEvent {

    @Id
    @GeneratedValue
    @UuidGenerator
    private UUID id;

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "transfer_id", nullable = false)
    private ShareTransfer transfer;

    @Enumerated(EnumType.STRING)
    @Column(name = "event_type", nullable = false, length = 30)
    private TransferEventType eventType;

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
