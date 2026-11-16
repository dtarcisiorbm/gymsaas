package com.gymsaas.tenant.application;

import com.gymsaas.audit.application.AuditService;
import com.gymsaas.shared.application.ApiException;
import com.gymsaas.shared.domain.BrDocuments;
import com.gymsaas.tenant.domain.Tenant;
import com.gymsaas.tenant.domain.TenantRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;
import java.util.UUID;

@Service
public class TenantApplicationService {

    private final TenantRepository tenantRepository;
    private final AuditService auditService;

    public TenantApplicationService(TenantRepository tenantRepository, AuditService auditService) {
        this.tenantRepository = tenantRepository;
        this.auditService = auditService;
    }

    @Transactional(readOnly = true)
    public Tenant current(UUID tenantId) {
        return tenantRepository.findById(tenantId)
                .orElseThrow(() -> ApiException.notFound("TENANT_NOT_FOUND", "Academia não encontrada."));
    }

    @Transactional
    public Tenant updateCurrent(UUID tenantId, TenantDtos.UpdateTenantRequest request) {
        if (!BrDocuments.validDocument(request.document())) {
            throw ApiException.badRequest("INVALID_DOCUMENT", "Documento deve ter 11 ou 14 dígitos.");
        }
        String document = BrDocuments.document(request.document());
        if (tenantRepository.existsByDocument(document, tenantId)) {
            throw ApiException.conflict("TENANT_DOCUMENT_EXISTS",
                    "Já existe uma academia com este documento.");
        }

        Tenant current = current(tenantId);
        Tenant updated = new Tenant(current.id(), request.name().trim(), request.legalName().trim(),
                document, request.email().trim().toLowerCase(), BrDocuments.phone(request.phone()),
                current.status(), current.createdAt(), current.updatedAt());
        Tenant saved = tenantRepository.save(updated);

        auditService.recordChange("TENANT_UPDATED", "tenant", saved.id(), "HIGH",
                Map.of("name", current.name(), "document", current.document()),
                Map.of("name", saved.name(), "document", saved.document()));

        return saved;
    }
}
