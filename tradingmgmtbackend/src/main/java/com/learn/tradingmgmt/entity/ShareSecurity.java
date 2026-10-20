package com.learn.tradingmgmt.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;

/**
 * Maps to the "securities" table. Named ShareSecurity (not "Security") to
 * avoid clashing with Spring Security types elsewhere in the codebase.
 */
@Entity
@Table(name = "securities")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ShareSecurity extends BaseAuditableEntity {

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "company_id", nullable = false)
    private Company company;

    @Column(name = "security_type", nullable = false)
    private String securityType = "EQUITY";

    private String isin;

    @Column(name = "face_value", precision = 18, scale = 4)
    private BigDecimal faceValue;

    @Column(name = "share_class")
    private String shareClass;

    @Column(name = "reference_price", precision = 18, scale = 4)
    private BigDecimal referencePrice;

    @Column(columnDefinition = "text")
    private String metadata;
}
