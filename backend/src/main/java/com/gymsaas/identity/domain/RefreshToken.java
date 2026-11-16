package com.gymsaas.identity.domain;

import java.time.Instant;
import java.util.UUID;

public record RefreshToken(
        UUID id,
        UUID userId,
        UUID tenantId,
        UUID familyId,
        String tokenHash,
        Instant issuedAt,
        Instant expiresAt,
        Instant revokedAt,
        String revokeReason,
        UUID replacedById) {

    public boolean active(Instant now) {
        return revokedAt == null && expiresAt.isAfter(now);
    }

    public RefreshToken revoked(String reason, Instant at, UUID replacedBy) {
        return new RefreshToken(id, userId, tenantId, familyId, tokenHash, issuedAt, expiresAt,
                at, reason, replacedBy);
    }
}
