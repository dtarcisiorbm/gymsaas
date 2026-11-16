package com.gymsaas.tenant.application;

import com.gymsaas.audit.application.AuditService;
import com.gymsaas.shared.application.ApiException;
import com.gymsaas.shared.domain.BrDocuments;
import com.gymsaas.shared.domain.PageResult;
import com.gymsaas.shared.domain.UuidV7;
import com.gymsaas.shared.infrastructure.tenant.TenantContext;
import com.gymsaas.tenant.domain.Tenant;
import com.gymsaas.tenant.domain.TenantRepository;
import com.gymsaas.tenant.domain.TenantStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

@Service
public class TenantAdminService {

    private final TenantRepository tenantRepository;
    private final AuditService auditService;

    public TenantAdminService(TenantRepository tenantRepository, AuditService auditService) {
        this.tenantRepository = tenantRepository;
        this.auditService = auditService;
    }

    @Transactional(readOnly = true)
    public PageResult<TenantDtos.TenantSummary> list(String status, String search, int page, int size) {
        TenantContext.requirePlatformAdmin();
        return toPage(tenantRepository.search(normalizeStatus(status), search, page, clampSize(size)));
    }

    @Transactional(readOnly = true)
    public TenantDtos.TenantDetail find(UUID tenantId) {
        TenantContext.requirePlatformAdmin();
        return TenantDtos.TenantDetail.from(requireTenant(tenantId));
    }

    @Transactional
    public TenantDtos.TenantDetail create(TenantDtos.CreateTenantRequest request) {
        TenantContext.requirePlatformAdmin();
        String document = normalizeDocument(request.document());
        if (tenantRepository.existsByDocument(document, null)) {
            throw ApiException.conflict("TENANT_DOCUMENT_EXISTS",
                    "Já existe uma academia com este documento.");
        }
        Instant now = Instant.now();
        Tenant tenant = new Tenant(UuidV7.generate(), request.name().trim(), request.legalName().trim(),
                document, request.email().trim().toLowerCase(), BrDocuments.phone(request.phone()),
                TenantStatus.PENDING.name(), now, now);
        Tenant saved = tenantRepository.save(tenant);

        auditService.recordForTenant(saved.id(), "TENANT_CREATED", "tenant", saved.id(), "HIGH",
                null, Map.of("name", saved.name(), "document", saved.document()), Map.of());

        return TenantDtos.TenantDetail.from(saved);
    }

    @Transactional
    public TenantDtos.TenantDetail update(UUID tenantId, TenantDtos.UpdateTenantRequest request) {
        TenantContext.requirePlatformAdmin();
        Tenant current = requireTenant(tenantId);
        String document = normalizeDocument(request.document());
        if (tenantRepository.existsByDocument(document, tenantId)) {
            throw ApiException.conflict("TENANT_DOCUMENT_EXISTS",
                    "Já existe uma academia com este documento.");
        }

        Tenant updated = new Tenant(current.id(), request.name().trim(), request.legalName().trim(),
                document, request.email().trim().toLowerCase(), BrDocuments.phone(request.phone()),
                current.status(), current.createdAt(), current.updatedAt());
        Tenant saved = tenantRepository.save(updated);

        auditService.recordChange("TENANT_UPDATED", "tenant", saved.id(), "HIGH",
                Map.of("name", current.name(), "document", current.document()),
                Map.of("name", saved.name(), "document", saved.document()));

        return TenantDtos.TenantDetail.from(saved);
    }

    @Transactional
    public TenantDtos.TenantDetail changeStatus(UUID tenantId,
                                                TenantDtos.ChangeTenantStatusRequest request) {
        TenantContext.requirePlatformAdmin();
        Tenant current = requireTenant(tenantId);
        TenantStatus status = TenantStatus.parse(request.status())
                .orElseThrow(() -> ApiException.badRequest("INVALID_STATUS", "Status inválido."));

        Tenant updated = new Tenant(current.id(), current.name(), current.legalName(),
                current.document(), current.email(), current.phone(), status.name(),
                current.createdAt(), current.updatedAt());
        Tenant saved = tenantRepository.save(updated);

        auditService.recordChange("TENANT_STATUS_CHANGED", "tenant", saved.id(), "HIGH",
                Map.of("status", current.status()), Map.of("status", saved.status()));

        return TenantDtos.TenantDetail.from(saved);
    }

    private Tenant requireTenant(UUID tenantId) {
        return tenantRepository.findById(tenantId)
                .orElseThrow(() -> ApiException.notFound("TENANT_NOT_FOUND", "Academia não encontrada."));
    }

    private String normalizeDocument(String value) {
        if (!BrDocuments.validDocument(value)) {
            throw ApiException.badRequest("INVALID_DOCUMENT", "Documento deve ter 11 ou 14 dígitos.");
        }
        return BrDocuments.document(value);
    }

    private String normalizeStatus(String status) {
        if (status == null || status.isBlank()) {
            return null;
        }
        return TenantStatus.parse(status)
                .map(Enum::name)
                .orElseThrow(() -> ApiException.badRequest("INVALID_STATUS", "Status inválido."));
    }

    private int clampSize(int size) {
        return Math.max(1, Math.min(size, 100));
    }

    private PageResult<TenantDtos.TenantSummary> toPage(PageResult<Tenant> page) {
        return PageResult.of(page.content().stream().map(TenantDtos.TenantSummary::from).toList(),
                page.totalElements(), page.page(), page.size());
    }
}
