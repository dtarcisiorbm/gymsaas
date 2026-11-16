package com.gymsaas.tenant.domain;

import java.time.Instant;
import java.util.UUID;

public record Tenant(
        UUID id,
        String name,
        String legalName,
        String document,
        String email,
        String phone,
        String status,
        Instant createdAt,
        Instant updatedAt) {

    public boolean active() {
        return "ACTIVE".equals(status);
    }
}
