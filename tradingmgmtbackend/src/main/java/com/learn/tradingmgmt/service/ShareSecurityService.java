package com.learn.tradingmgmt.service;

import com.learn.tradingmgmt.dto.request.ShareSecurityRequest;
import com.learn.tradingmgmt.entity.ShareSecurity;

import java.util.List;
import java.util.UUID;

public interface ShareSecurityService {
    ShareSecurity create(ShareSecurityRequest request, UUID actingUserId);
    ShareSecurity get(UUID id);
    List<ShareSecurity> listByCompany(UUID companyId);
    List<ShareSecurity> list();
}
