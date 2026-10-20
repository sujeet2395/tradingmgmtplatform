package com.learn.tradingmgmt.service.impl;

import com.learn.tradingmgmt.dto.request.CustomerRequest;
import com.learn.tradingmgmt.entity.Customer;
import com.learn.tradingmgmt.entity.enums.DocumentCategory;
import com.learn.tradingmgmt.entity.enums.DocumentVerificationStatus;
import com.learn.tradingmgmt.exception.InvalidStateTransitionException;
import com.learn.tradingmgmt.exception.ResourceNotFoundException;
import com.learn.tradingmgmt.repository.CustomerRepository;
import com.learn.tradingmgmt.repository.DocumentRepository;
import com.learn.tradingmgmt.service.AuditLogService;
import com.learn.tradingmgmt.service.CustomerService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class CustomerServiceImpl implements CustomerService {

    private final CustomerRepository customerRepository;
    private final AuditLogService auditLogService;
    private final DocumentRepository documentRepository;

    @Override
    @Transactional
    public Customer create(CustomerRequest request, UUID actingUserId) {
        Customer customer = new Customer();
        apply(customer, request);
        customer.setCreatedBy(actingUserId);
        customer.setUpdatedBy(actingUserId);
        Customer saved = customerRepository.save(customer);
        auditLogService.record(actingUserId, "CUSTOMER_CREATE", "Customer", saved.getId(), null, saved, null, "API");
        return saved;
    }

    @Override
    public Customer get(UUID id) {
        return customerRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Customer", id));
    }

    @Override
    public List<Customer> list() {
        return customerRepository.findAll();
    }

    @Override
    @Transactional
    public Customer update(UUID id, CustomerRequest request, UUID actingUserId) {
        Customer customer = get(id);
        Customer before = copy(customer);
        apply(customer, request);
        customer.setUpdatedBy(actingUserId);
        Customer saved = customerRepository.save(customer);
        auditLogService.record(actingUserId, "CUSTOMER_UPDATE", "Customer", id, before, saved, null, "API");
        return saved;
    }

    private void apply(Customer customer, CustomerRequest request) {
        customer.setFullName(request.fullName());
        customer.setPhone(request.phone());
        customer.setEmail(request.email());
        customer.setAddress(request.address());
        customer.setBankAccountName(request.bankAccountName());
        customer.setBankIfsc(request.bankIfsc());
    }

    private Customer copy(Customer source) {
        Customer c = new Customer();
        c.setFullName(source.getFullName());
        c.setPhone(source.getPhone());
        c.setEmail(source.getEmail());
        c.setAddress(source.getAddress());
        c.setBankAccountName(source.getBankAccountName());
        c.setBankIfsc(source.getBankIfsc());
        return c;
    }
    @Override
    @Transactional
    public Customer updateKyc(UUID id, boolean verified, UUID userId) {
        Customer c = get(id);
        if (verified) {
            boolean hasVerifiedDoc = documentRepository
                .findByRelatedEntityTypeAndRelatedEntityId("Customer", id).stream()
                .anyMatch(d -> d.getCategory() == DocumentCategory.KYC
                    && d.getCurrentVersion() != null
                    && d.getCurrentVersion().getVerificationStatus() == DocumentVerificationStatus.VERIFIED);
            if (!hasVerifiedDoc)
                throw new InvalidStateTransitionException("Verify at least one KYC document first");
        }
        boolean before = c.getKycVerified();
        c.setKycVerified(verified);
        c.setUpdatedBy(userId);
        Customer saved = customerRepository.save(c);
        auditLogService.record(userId, verified ? "KYC_VERIFY" : "KYC_REVOKE",
                "Customer", id, before, verified, null, "API");
        return saved;
    }
}
