package com.gymsaas.tenant.presentation;

import com.gymsaas.branch.application.BranchDtos;
import com.gymsaas.branch.application.BranchService;
import com.gymsaas.shared.domain.PageResult;
import com.gymsaas.tenant.application.TenantAdminService;
import com.gymsaas.tenant.application.TenantDtos;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

/** Global tenant administration. Only platform administrators hold these permissions. */
@RestController
@RequestMapping("/api/admin/tenants")
@Tag(name = "Administração SaaS")
public class TenantAdminController {

    private final TenantAdminService tenantAdminService;
    private final BranchService branchService;

    public TenantAdminController(TenantAdminService tenantAdminService, BranchService branchService) {
        this.tenantAdminService = tenantAdminService;
        this.branchService = branchService;
    }

    @GetMapping
    @PreAuthorize("hasAuthority('tenants:read')")
    @Operation(summary = "Lista academias com filtros e paginação")
    public PageResult<TenantDtos.TenantSummary> list(
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String search,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return tenantAdminService.list(status, search, page, size);
    }

    @PostMapping
    @PreAuthorize("hasAuthority('tenants:create')")
    @Operation(summary = "Cadastra uma academia")
    public ResponseEntity<TenantDtos.TenantDetail> create(
            @Valid @RequestBody TenantDtos.CreateTenantRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(tenantAdminService.create(request));
    }

    @GetMapping("/{tenantId}")
    @PreAuthorize("hasAuthority('tenants:read')")
    public TenantDtos.TenantDetail detail(@PathVariable UUID tenantId) {
        return tenantAdminService.find(tenantId);
    }

    @PutMapping("/{tenantId}")
    @PreAuthorize("hasAuthority('tenants:update')")
    public TenantDtos.TenantDetail update(@PathVariable UUID tenantId,
                                          @Valid @RequestBody TenantDtos.UpdateTenantRequest request) {
        return tenantAdminService.update(tenantId, request);
    }

    @PatchMapping("/{tenantId}/status")
    @PreAuthorize("hasAuthority('tenants:suspend')")
    @Operation(summary = "Altera o status da academia (ativação/suspensão)")
    public TenantDtos.TenantDetail changeStatus(
            @PathVariable UUID tenantId,
            @Valid @RequestBody TenantDtos.ChangeTenantStatusRequest request) {
        return tenantAdminService.changeStatus(tenantId, request);
    }

    @GetMapping("/{tenantId}/branches")
    @PreAuthorize("hasAuthority('branches:read') or hasAuthority('tenants:read')")
    @Operation(summary = "Consulta as unidades de uma academia")
    public PageResult<BranchDtos.BranchSummary> branches(
            @PathVariable UUID tenantId,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String search,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return branchService.listForTenantAsPlatformAdmin(tenantId, status, search, page, size);
    }
}
