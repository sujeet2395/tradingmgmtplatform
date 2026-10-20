package com.learn.tradingmgmt.service.impl;

import com.learn.tradingmgmt.dto.request.CompanyRequest;
import com.learn.tradingmgmt.entity.Company;
import com.learn.tradingmgmt.exception.ResourceNotFoundException;
import com.learn.tradingmgmt.repository.CompanyRepository;
import com.learn.tradingmgmt.service.AuditLogService;
import com.learn.tradingmgmt.service.CompanyService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class CompanyServiceImpl implements CompanyService {

    private final CompanyRepository companyRepository;
    private final AuditLogService auditLogService;

    @Override
    @Transactional
    public Company create(CompanyRequest request, UUID actingUserId) {
        Company company = new Company();
        company.setLegalName(request.legalName());
        company.setCin(request.cin());
        if (request.status() != null) company.setStatus(request.status());
        company.setCreatedBy(actingUserId);
        company.setUpdatedBy(actingUserId);
        Company saved = companyRepository.save(company);
        auditLogService.record(actingUserId, "COMPANY_CREATE", "Company", saved.getId(), null, saved, null, "API");
        return saved;
    }

    @Override
    public Company get(UUID id) {
        return companyRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Company", id));
    }

    @Override
    public List<Company> list() {
        return companyRepository.findAll();
    }

    @Override
    @Transactional
    public Company update(UUID id, CompanyRequest request, UUID actingUserId) {
        Company company = get(id);
        company.setLegalName(request.legalName());
        company.setCin(request.cin());
        if (request.status() != null) company.setStatus(request.status());
        company.setUpdatedBy(actingUserId);
        Company saved = companyRepository.save(company);
        auditLogService.record(actingUserId, "COMPANY_UPDATE", "Company", id, null, saved, null, "API");
        return saved;
    }
}
