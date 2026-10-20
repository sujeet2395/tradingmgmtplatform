package com.learn.tradingmgmt.dto.response;

import com.learn.tradingmgmt.entity.*;
import com.learn.tradingmgmt.entity.enums.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

public record OrderResponse(
        UUID id, String orderNumber, OrderType orderType,
        CustomerRef buyerCustomer, CustomerRef sellerCustomer, SecurityRef security,
        BigDecimal quantity, BigDecimal pricePerShare, BigDecimal grossAmount,
        String currency, LocalDate orderDate, OrderStatus status, String notes) {

    public record CustomerRef(UUID id, String fullName) {}
    public record CompanyRef(UUID id, String legalName) {}
    public record SecurityRef(UUID id, String securityType, String shareClass,
                              String isin, CompanyRef company) {}

    public static OrderResponse from(Order o) {
        return new OrderResponse(o.getId(), o.getOrderNumber(), o.getOrderType(),
                customer(o.getBuyerCustomer()), customer(o.getSellerCustomer()),
                security(o.getSecurity()), o.getQuantity(), o.getPricePerShare(),
                o.getGrossAmount(), o.getCurrency(), o.getOrderDate(),
                o.getStatus(), o.getNotes());
    }

    private static CustomerRef customer(Customer c) {
        return c == null ? null : new CustomerRef(c.getId(), c.getFullName());
    }

    private static SecurityRef security(ShareSecurity s) {
        Company c = s.getCompany();
        return new SecurityRef(s.getId(), s.getSecurityType(), s.getShareClass(),
                s.getIsin(), new CompanyRef(c.getId(), c.getLegalName()));
    }
}