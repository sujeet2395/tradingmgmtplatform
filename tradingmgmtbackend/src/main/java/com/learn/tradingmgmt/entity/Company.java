package com.learn.tradingmgmt.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "companies")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Company extends BaseAuditableEntity {

    @Column(name = "legal_name", nullable = false, unique = true)
    private String legalName;

    private String cin;

    @Column(nullable = false)
    private String status = "ACTIVE";

    // Stored as raw JSON text for now; swap to a JsonNode + hypersistence-utils
    // converter later if you want native jsonb querying.
    @Column(name = "contact_info", columnDefinition = "text")
    private String contactInfo;

    @Column(name = "transfer_requirements", columnDefinition = "text")
    private String transferRequirements;

    @Column(columnDefinition = "text")
    private String metadata;
}
