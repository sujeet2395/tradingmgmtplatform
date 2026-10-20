package com.learn.tradingmgmt.controller;

import com.learn.tradingmgmt.dto.request.CustomerRequest;
import com.learn.tradingmgmt.entity.Customer;
import com.learn.tradingmgmt.service.CustomerService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/customers")
@RequiredArgsConstructor
public class CustomerController {

    private final CustomerService customerService;

    @PostMapping
    public ResponseEntity<Customer> create(@Valid @RequestBody CustomerRequest request,
                                            @RequestHeader(value = "X-User-Id", required = false) UUID actingUserId) {
        return ResponseEntity.status(HttpStatus.CREATED).body(customerService.create(request, actingUserId));
    }

    @GetMapping
    public List<Customer> list() {
        return customerService.list();
    }

    @Operation(summary = "Get customer by id")
    @GetMapping("/{id}")
    public Customer get(@Parameter(description = "Customer UUID", required = true) @PathVariable("id") UUID id) {
        return customerService.get(id);
    }

    @Operation(summary = "Update customer by id")
    @PutMapping("/{id}")
    public Customer update(@Parameter(description = "Customer UUID", required = true) @PathVariable UUID id, @Valid @RequestBody CustomerRequest request,
                            @RequestHeader(value = "X-User-Id", required = false) UUID actingUserId) {
        return customerService.update(id, request, actingUserId);
    }

    @Operation(summary = "Verify customer KYC")
    @PostMapping("/{id}/kyc/verify")
    public Customer verifyKyc(@PathVariable UUID id,
            @RequestHeader(value = "X-User-Id", required = false) UUID u) {
        return customerService.updateKyc(id, true, u);
    }

    @Operation(summary = "Revoke customer KYC")
    @PostMapping("/{id}/kyc/revoke")
    public Customer revokeKyc(@PathVariable UUID id,
            @RequestHeader(value = "X-User-Id", required = false) UUID u) {
        return customerService.updateKyc(id, false, u);
    }
}
