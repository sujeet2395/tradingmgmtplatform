package com.learn.tradingmgmt.repository;

import com.learn.tradingmgmt.entity.ShareSecurity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface ShareSecurityRepository extends JpaRepository<ShareSecurity, UUID> {
    List<ShareSecurity> findByCompanyId(UUID companyId);
}
