package com.learn.tradingmgmt.entity;

import com.learn.tradingmgmt.entity.enums.OrderStatus;
import com.learn.tradingmgmt.entity.enums.OrderType;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Represents commercial INTENTION (not settlement). See Transaction for
 * the actual settlement record created once an order is confirmed.
 */
@Entity
@Table(name = "orders")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Order extends BaseAuditableEntity {

    @Column(name = "order_number", nullable = false, unique = true)
    private String orderNumber;

    @Enumerated(EnumType.STRING)
    @Column(name = "order_type", nullable = false, length = 10)
    private OrderType orderType;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "buyer_customer_id")
    private Customer buyerCustomer;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "seller_customer_id")
    private Customer sellerCustomer;

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "security_id", nullable = false)
    private ShareSecurity security;

    @Column(nullable = false, precision = 18, scale = 4)
    private BigDecimal quantity;

    @Column(name = "price_per_share", nullable = false, precision = 18, scale = 4)
    private BigDecimal pricePerShare;

    @Column(name = "gross_amount", nullable = false, precision = 18, scale = 4)
    private BigDecimal grossAmount;

    @Column(nullable = false, length = 10)
    private String currency = "INR";

    @Column(name = "order_date", nullable = false)
    private LocalDate orderDate;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private OrderStatus status = OrderStatus.DRAFT;

    @Column(columnDefinition = "text")
    private String notes;
}
