package com.gymsaas.audit.application;

import com.gymsaas.audit.domain.AuditLog;
import com.gymsaas.audit.domain.AuditRepository;
import com.gymsaas.shared.domain.PageResult;
import com.gymsaas.shared.domain.UuidV7;
import com.gymsaas.shared.infrastructure.tenant.TenantContext;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

@Service
public class AuditService {

    private final AuditRepository repository;

    public AuditService(AuditRepository repository) {
        this.repository = repository;
    }

    public void record(String action, String resourceType, UUID resourceId, String riskLevel,
                       Map<String, Object> metadata) {
        recordForTenant(TenantContext.tenantId(), action, resourceType, resourceId, riskLevel,
                null, null, metadata);
    }

    public void recordChange(String action, String resourceType, UUID resourceId, String riskLevel,
                             Map<String, Object> oldValues, Map<String, Object> newValues) {
        recordForTenant(TenantContext.tenantId(), action, resourceType, resourceId, riskLevel,
                oldValues, newValues, Map.of());
    }

    public void recordForTenant(UUID tenantId, String action, String resourceType, UUID resourceId,
                                String riskLevel, Map<String, Object> oldValues,
                                Map<String, Object> newValues, Map<String, Object> metadata) {
        UUID actorId = TenantContext.userId();
        repository.append(new AuditLog(
                UuidV7.generate(),
                tenantId,
                actorId == null ? "SYSTEM" : "USER",
                actorId,
                action,
                resourceType,
                resourceId,
                oldValues,
                newValues,
                metadata == null ? Map.of() : metadata,
                riskLevel,
                Instant.now()));
    }

    public PageResult<AuditLog> search(UUID tenantId, int page, int size) {
        return repository.findByTenant(tenantId, page, size);
    }
}
