package com.learn.tradingmgmt.entity;

import jakarta.persistence.*;
import lombok.*;

/**
 * Buyer / seller / broker. Role is assigned per-transaction via
 * {@link TransactionParty}, since the same customer can buy in one deal
 * and sell in another.
 *
 * NOTE: panEncrypted / bankAccountNumberEncrypted must be encrypted at the
 * application layer (e.g. a JPA AttributeConverter with AES-GCM) before
 * being persisted — this scaffold stores raw bytes as a placeholder only.
 */
@Entity
@Table(name = "customers")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Customer extends BaseAuditableEntity {

    @Column(name = "full_name", nullable = false)
    private String fullName;

    @Lob
    @Column(name = "pan_encrypted")
    private byte[] panEncrypted;

    private String phone;

    private String email;

    @Column(columnDefinition = "text")
    private String address;

    @Column(name = "bank_account_name")
    private String bankAccountName;

    @Lob
    @Column(name = "bank_account_number_encrypted")
    private byte[] bankAccountNumberEncrypted;

    @Column(name = "bank_ifsc")
    private String bankIfsc;

    @Column(name = "kyc_verified", nullable = false)
    private Boolean kycVerified = false;

    @Column(columnDefinition = "text")
    private String notes;
}
