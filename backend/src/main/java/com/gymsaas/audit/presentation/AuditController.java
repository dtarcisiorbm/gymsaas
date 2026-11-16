package com.gymsaas.audit.presentation;

import com.gymsaas.audit.application.AuditService;
import com.gymsaas.audit.domain.AuditLog;
import com.gymsaas.shared.domain.PageResult;
import com.gymsaas.shared.infrastructure.tenant.TenantContext;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/audit-logs")
public class AuditController {

    private final AuditService auditService;

    public AuditController(AuditService auditService) {
        this.auditService = auditService;
    }

    @GetMapping
    @PreAuthorize("hasAuthority('audit:read')")
    public PageResult<AuditLogResponse> list(@RequestParam(defaultValue = "0") int page,
                                             @RequestParam(defaultValue = "20") int size) {
        int safePage = Math.max(page, 0);
        int safeSize = Math.min(Math.max(size, 1), 100);
        PageResult<AuditLog> result = auditService.search(TenantContext.requireTenantId(), safePage, safeSize);
        return PageResult.of(result.content().stream().map(AuditLogResponse::from).toList(),
                result.totalElements(), result.page(), result.size());
    }

    public record AuditLogResponse(UUID id, UUID tenantId, String actorType, UUID actorId,
                                   String action, String resourceType, UUID resourceId,
                                   Map<String, Object> oldValues, Map<String, Object> newValues,
                                   Map<String, Object> metadata, String riskLevel, Instant occurredAt) {

        static AuditLogResponse from(AuditLog log) {
            return new AuditLogResponse(log.id(), log.tenantId(), log.actorType(), log.actorId(),
                    log.action(), log.resourceType(), log.resourceId(), log.oldValues(),
                    log.newValues(), log.metadata(), log.riskLevel(), log.occurredAt());
        }
    }
}
