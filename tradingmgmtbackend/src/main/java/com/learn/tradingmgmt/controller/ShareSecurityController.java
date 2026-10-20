package com.learn.tradingmgmt.controller;

import com.learn.tradingmgmt.dto.request.ShareSecurityRequest;
import com.learn.tradingmgmt.entity.ShareSecurity;
import com.learn.tradingmgmt.service.ShareSecurityService;
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
@RequestMapping("/api/securities")
@RequiredArgsConstructor
public class ShareSecurityController {

    private final ShareSecurityService shareSecurityService;

    @PostMapping
    public ResponseEntity<ShareSecurity> create(@Valid @RequestBody ShareSecurityRequest request,
                                                 @RequestHeader(value = "X-User-Id", required = false) UUID actingUserId) {
        return ResponseEntity.status(HttpStatus.CREATED).body(shareSecurityService.create(request, actingUserId));
    }

    @Operation(summary = "List securities, optionally filtered by companyId")
    @GetMapping
    public List<ShareSecurity> list(
            @Parameter(description = "Optional company UUID to filter securities", required = false)
            @RequestParam(name = "companyId", required = false) UUID companyId) {
        return companyId != null ? shareSecurityService.listByCompany(companyId) : shareSecurityService.list();
    }

    @Operation(summary = "Get security by id")
    @GetMapping("/{id}")
    public ShareSecurity get(@Parameter(description = "Security UUID", required = true) @PathVariable("id") UUID id) {
        return shareSecurityService.get(id);
    }
}
