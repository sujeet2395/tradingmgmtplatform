package com.learn.tradingmgmt.service;

import com.learn.tradingmgmt.dto.request.CompanyRequest;
import com.learn.tradingmgmt.entity.Company;

import java.util.List;
import java.util.UUID;

public interface CompanyService {
    Company create(CompanyRequest request, UUID actingUserId);
    Company get(UUID id);
    List<Company> list();
    Company update(UUID id, CompanyRequest request, UUID actingUserId);
}
