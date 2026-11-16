package com.gymsaas.identity.domain;

import java.util.Optional;
import java.util.UUID;

public interface RefreshTokenRepository {

    Optional<RefreshToken> findByTokenHash(String tokenHash);

    RefreshToken save(RefreshToken token);

    int revokeFamily(UUID familyId, String reason);

    void updateTenantForFamily(UUID familyId, UUID tenantId);
}
