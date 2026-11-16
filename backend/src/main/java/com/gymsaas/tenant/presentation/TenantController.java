package com.gymsaas.tenant.presentation;

import com.gymsaas.shared.domain.PageResult;
import com.gymsaas.shared.infrastructure.tenant.TenantContext;
import com.gymsaas.tenant.application.TenantAdminService;
import com.gymsaas.tenant.application.TenantDtos;
import com.gymsaas.tenant.application.TenantApplicationService;
import com.gymsaas.tenant.domain.Tenant;
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
@RequestMapping("/api/tenants")
@Tag(name = "Academias")
public class TenantController {

    private final TenantApplicationService tenantService;
    private final TenantAdminService tenantAdminService;

    public TenantController(TenantApplicationService tenantService, TenantAdminService tenantAdminService) {
        this.tenantService = tenantService;
        this.tenantAdminService = tenantAdminService;
    }

    @GetMapping("/current")
    @PreAuthorize("hasAuthority('tenant:read') or hasAuthority('tenants:read')")
    @Operation(summary = "Dados cadastrais da academia ativa")
    public TenantResponse current() {
        return TenantResponse.from(tenantService.current(TenantContext.requireTenantId()));
    }

    @PutMapping("/current")
    @PreAuthorize("hasAuthority('tenant:update') or hasAuthority('tenants:update')")
    @Operation(summary = "Atualiza os dados cadastrais da academia ativa")
    public TenantResponse updateCurrent(@Valid @RequestBody TenantDtos.UpdateTenantRequest request) {
        return TenantResponse.from(tenantService.updateCurrent(
                TenantContext.requireTenantId(), request));
    }

    public record TenantResponse(UUID id, String name, String legalName, String document, String cnpj,
                                 String email, String phone, String status,
                                 java.time.Instant createdAt, java.time.Instant updatedAt) {

        static TenantResponse from(Tenant tenant) {
            return new TenantResponse(tenant.id(), tenant.name(), tenant.legalName(),
                    tenant.document(), tenant.document(), tenant.email(), tenant.phone(),
                    tenant.status(), tenant.createdAt(), tenant.updatedAt());
        }
    }
}
