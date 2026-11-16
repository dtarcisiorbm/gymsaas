package com.gymsaas.membership.domain;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

/** Append-only lifecycle entry (membership_status_history). */
public record MembershipEvent(
        UUID id,
        UUID tenantId,
        UUID membershipId,
        MembershipEventType type,
        MembershipStatus fromStatus,
        MembershipStatus toStatus,
        String reason,
        Map<String, Object> metadata,
        UUID changedBy,
        Instant changedAt) {

    public MembershipEvent {
        metadata = metadata == null ? Map.of() : Map.copyOf(metadata);
    }
}
