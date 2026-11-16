package com.gymsaas.identity.domain;

import java.time.Instant;
import java.util.UUID;

public record User(
        UUID id,
        String email,
        String passwordHash,
        String fullName,
        String phone,
        String status,
        boolean mfaEnabled,
        boolean platformAdmin,
        Instant lastLoginAt,
        Instant createdAt) {

    public boolean active() {
        return "ACTIVE".equals(status);
    }

    public User withLastLogin(Instant instant) {
        return new User(id, email, passwordHash, fullName, phone, status, mfaEnabled,
                platformAdmin, instant, createdAt);
    }
}
