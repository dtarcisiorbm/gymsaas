package com.gymsaas.branch.presentation;

import com.gymsaas.branch.application.BranchDtos;
import com.gymsaas.branch.application.BranchService;
import com.gymsaas.shared.domain.PageResult;
import com.gymsaas.shared.infrastructure.tenant.TenantContext;
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

@RestController
@RequestMapping("/api/branches")
@Tag(name = "Unidades")
public class BranchController {

    private final BranchService branchService;

    public BranchController(BranchService branchService) {
        this.branchService = branchService;
    }

    @GetMapping
    @PreAuthorize("hasAuthority('branches:read')")
    @Operation(summary = "Lista as unidades da academia ativa")
    public PageResult<BranchDtos.BranchSummary> list(
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String search,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return branchService.list(TenantContext.requireTenantId(), status, search, page, size);
    }

    @PostMapping
    @PreAuthorize("hasAuthority('branches:create')")
    @Operation(summary = "Cadastra uma unidade na academia ativa")
    public ResponseEntity<BranchDtos.BranchDetail> create(
            @Valid @RequestBody BranchDtos.CreateBranchRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(branchService.create(TenantContext.requireTenantId(), request));
    }

    @GetMapping("/{branchId}")
    @PreAuthorize("hasAuthority('branches:read')")
    public BranchDtos.BranchDetail detail(@PathVariable UUID branchId) {
        return branchService.find(TenantContext.requireTenantId(), branchId);
    }

    @PutMapping("/{branchId}")
    @PreAuthorize("hasAuthority('branches:update')")
    public BranchDtos.BranchDetail update(@PathVariable UUID branchId,
                                          @Valid @RequestBody BranchDtos.UpdateBranchRequest request) {
        return branchService.update(TenantContext.requireTenantId(), branchId, request);
    }

    @PatchMapping("/{branchId}/status")
    @PreAuthorize("hasAuthority('branches:deactivate')")
    @Operation(summary = "Ativa ou desativa uma unidade")
    public BranchDtos.BranchDetail changeStatus(
            @PathVariable UUID branchId,
            @Valid @RequestBody BranchDtos.ChangeBranchStatusRequest request) {
        return branchService.changeStatus(TenantContext.requireTenantId(), branchId, request);
    }

    @GetMapping("/{branchId}/settings")
    @PreAuthorize("hasAuthority('branches:read')")
    public BranchDtos.BranchSettingsView settings(@PathVariable UUID branchId) {
        return branchService.settings(TenantContext.requireTenantId(), branchId);
    }

    @PutMapping("/{branchId}/settings")
    @PreAuthorize("hasAuthority('branches:update')")
    public BranchDtos.BranchSettingsView updateSettings(
            @PathVariable UUID branchId,
            @Valid @RequestBody BranchDtos.UpdateBranchSettingsRequest request) {
        return branchService.updateSettings(TenantContext.requireTenantId(), branchId, request);
    }
}
