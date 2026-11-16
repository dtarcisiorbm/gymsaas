package com.gymsaas.shared.infrastructure.security;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.UUID;

/**
 * Short-lived revocation list for refresh-token families. Access tokens stay
 * valid for their full TTL by design; this cache closes the gap immediately on
 * logout or refresh-token reuse detection.
 */
@Service
public class RevokedSessionService {

    private final Cache<UUID, Boolean> revoked;

    public RevokedSessionService(SecurityProperties properties) {
        Duration ttl = properties.jwt().accessTokenTtl() == null
                ? Duration.ofMinutes(15)
                : properties.jwt().accessTokenTtl();
        this.revoked = Caffeine.newBuilder()
                .expireAfterWrite(ttl)
                .maximumSize(100_000)
                .build();
    }

    public void revoke(UUID familyId) {
        if (familyId != null) {
            revoked.put(familyId, Boolean.TRUE);
        }
    }

    public boolean isRevoked(String familyId) {
        if (familyId == null) {
            return false;
        }
        try {
            return revoked.getIfPresent(UUID.fromString(familyId)) != null;
        } catch (IllegalArgumentException exception) {
            return true;
        }
    }

    public void clear() {
        revoked.invalidateAll();
    }
}
