package com.gymsaas.identity.infrastructure.persistence;

import com.gymsaas.identity.domain.TenantMember;

import java.util.UUID;

public record TenantMemberProjection(
        UUID userId,
        String email,
        String fullName,
        String status,
        UUID membershipId,
        boolean defaultMembership) {

    public TenantMember toDomain() {
        return new TenantMember(userId, email, fullName, status, membershipId, defaultMembership);
    }
}
