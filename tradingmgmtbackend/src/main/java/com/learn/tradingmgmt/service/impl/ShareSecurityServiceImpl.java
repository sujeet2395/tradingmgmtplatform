package com.learn.tradingmgmt.service.impl;

import com.learn.tradingmgmt.dto.request.ShareSecurityRequest;
import com.learn.tradingmgmt.entity.Company;
import com.learn.tradingmgmt.entity.ShareSecurity;
import com.learn.tradingmgmt.exception.ResourceNotFoundException;
import com.learn.tradingmgmt.repository.CompanyRepository;
import com.learn.tradingmgmt.repository.ShareSecurityRepository;
import com.learn.tradingmgmt.service.AuditLogService;
import com.learn.tradingmgmt.service.ShareSecurityService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ShareSecurityServiceImpl implements ShareSecurityService {

    private final ShareSecurityRepository shareSecurityRepository;
    private final CompanyRepository companyRepository;
    private final AuditLogService auditLogService;

    @Override
    @Transactional
    public ShareSecurity create(ShareSecurityRequest request, UUID actingUserId) {
        Company company = companyRepository.findById(request.companyId())
                .orElseThrow(() -> new ResourceNotFoundException("Company", request.companyId()));

        ShareSecurity security = new ShareSecurity();
        security.setCompany(company);
        security.setSecurityType(request.securityType() != null ? request.securityType() : "EQUITY");
        security.setIsin(request.isin());
        security.setFaceValue(request.faceValue());
        security.setShareClass(request.shareClass());
        security.setReferencePrice(request.referencePrice());
        security.setCreatedBy(actingUserId);
        security.setUpdatedBy(actingUserId);

        ShareSecurity saved = shareSecurityRepository.save(security);
        auditLogService.record(actingUserId, "SECURITY_CREATE", "ShareSecurity", saved.getId(), null, saved, null, "API");
        return saved;
    }

    @Override
    public ShareSecurity get(UUID id) {
        return shareSecurityRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("ShareSecurity", id));
    }

    @Override
    public List<ShareSecurity> listByCompany(UUID companyId) {
        return shareSecurityRepository.findByCompanyId(companyId);
    }

    @Override
    public List<ShareSecurity> list() {
        return shareSecurityRepository.findAll();
    }
}
