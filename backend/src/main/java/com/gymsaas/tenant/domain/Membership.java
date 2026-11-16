package com.gymsaas.tenant.domain;

import java.util.UUID;

public record Membership(
        UUID id,
        UUID userId,
        UUID tenantId,
        String tenantName,
        String tenantStatus,
        boolean defaultMembership,
        String status) {

    public boolean active() {
        return "ACTIVE".equals(status);
    }

    public boolean tenantActive() {
        return "ACTIVE".equals(tenantStatus);
    }
}
