package com.learn.tradingmgmt.controller;

import com.learn.tradingmgmt.dto.request.CompanyRequest;
import com.learn.tradingmgmt.entity.Company;
import com.learn.tradingmgmt.service.CompanyService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/companies")
@RequiredArgsConstructor
public class CompanyController {

    private final CompanyService companyService;

    @PostMapping
    public ResponseEntity<Company> create(@Valid @RequestBody CompanyRequest request,
                                           @RequestHeader(value = "X-User-Id", required = false) UUID actingUserId) {
        return ResponseEntity.status(HttpStatus.CREATED).body(companyService.create(request, actingUserId));
    }

    @GetMapping
    public List<Company> list() {
        return companyService.list();
    }

    @GetMapping("/{id}")
    public Company get(@PathVariable UUID id) {
        return companyService.get(id);
    }

    @PutMapping("/{id}")
    public Company update(@PathVariable UUID id, @Valid @RequestBody CompanyRequest request,
                           @RequestHeader(value = "X-User-Id", required = false) UUID actingUserId) {
        return companyService.update(id, request, actingUserId);
    }
}
