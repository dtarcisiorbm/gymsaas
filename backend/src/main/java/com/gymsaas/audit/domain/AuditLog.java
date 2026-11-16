package com.gymsaas.audit.domain;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

public record AuditLog(
        UUID id,
        UUID tenantId,
        String actorType,
        UUID actorId,
        String action,
        String resourceType,
        UUID resourceId,
        Map<String, Object> oldValues,
        Map<String, Object> newValues,
        Map<String, Object> metadata,
        String riskLevel,
        Instant occurredAt) {
}
