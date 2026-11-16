package com.gymsaas.membership.application;

import com.gymsaas.audit.application.AuditService;
import com.gymsaas.membership.domain.Membership;
import com.gymsaas.membership.domain.MembershipEvent;
import com.gymsaas.membership.domain.MembershipEventType;
import com.gymsaas.membership.domain.MembershipHistoryRepository;
import com.gymsaas.membership.domain.MembershipStatus;
import com.gymsaas.shared.domain.UuidV7;
import com.gymsaas.shared.infrastructure.tenant.TenantContext;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

/**
 * Records every lifecycle change twice: in the membership history (business
 * timeline shown to users) and in the platform audit log (append-only).
 * Neither contains sensitive personal data.
 */
@Component
public class MembershipJournal {

    private final MembershipHistoryRepository history;
    private final AuditService audit;

    public MembershipJournal(MembershipHistoryRepository history, AuditService audit) {
        this.history = history;
        this.audit = audit;
    }

    public void record(Membership membership, MembershipEventType type, MembershipStatus from, MembershipStatus to,
                       String reason, Map<String, Object> metadata, Instant at, String riskLevel) {
        Map<String, Object> safeMetadata = metadata == null ? Map.of() : metadata;
        history.append(new MembershipEvent(UuidV7.generate(), membership.tenantId(), membership.id(), type, from, to,
                reason, safeMetadata, TenantContext.userId(), at));

        Map<String, Object> auditMetadata = new HashMap<>(safeMetadata);
        auditMetadata.put("registrationNumber", membership.registrationNumber());
        if (reason != null) {
            auditMetadata.put("reason", reason);
        }
        audit.recordForTenant(membership.tenantId(), "MEMBERSHIP_" + type.name(), "membership", membership.id(),
                riskLevel, from == null ? null : Map.of("status", from.name()),
                to == null ? null : Map.of("status", to.name()), auditMetadata);
    }
}
