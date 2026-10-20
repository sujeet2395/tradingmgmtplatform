package com.learn.tradingmgmt.service;

import com.learn.tradingmgmt.dto.request.CustomerRequest;
import com.learn.tradingmgmt.entity.Customer;

import java.util.List;
import java.util.UUID;

public interface CustomerService {
    Customer create(CustomerRequest request, UUID actingUserId);
    Customer get(UUID id);
    List<Customer> list();
    Customer update(UUID id, CustomerRequest request, UUID actingUserId);
    Customer updateKyc(UUID id, boolean verified, UUID actingUserId);
}
