package com.gymsaas.tenant.infrastructure.persistence;

public record MembershipProjection(
        java.util.UUID id,
        java.util.UUID userId,
        java.util.UUID tenantId,
        String tenantName,
        String tenantStatus,
        boolean defaultMembership,
        String status) {
}
