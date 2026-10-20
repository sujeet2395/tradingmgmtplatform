package com.learn.tradingmgmt.repository;

import com.learn.tradingmgmt.entity.Company;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface CompanyRepository extends JpaRepository<Company, UUID> {
    Optional<Company> findByLegalName(String legalName);
}
