package com.gymsaas.billing.application;

import com.gymsaas.audit.application.AuditService;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.UUID;

/** Every financial action goes to the append-only audit log, without sensitive personal data. */
@Component
public class BillingJournal {

    private final AuditService audit;

    public BillingJournal(AuditService audit) {
        this.audit = audit;
    }

    public void record(UUID tenantId, String action, String resourceType, UUID resourceId, String riskLevel,
                       String fromStatus, String toStatus, Map<String, Object> metadata) {
        audit.recordForTenant(tenantId, action, resourceType, resourceId, riskLevel,
                fromStatus == null ? null : Map.of("status", fromStatus),
                toStatus == null ? null : Map.of("status", toStatus),
                metadata == null ? Map.of() : metadata);
    }

    public static String trimToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    /** Keeps generated reasons within the column limit. */
    public static String limit(String value, int max) {
        return value == null || value.length() <= max ? value : value.substring(0, max);
    }
}
