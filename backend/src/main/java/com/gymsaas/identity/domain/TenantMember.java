package com.gymsaas.identity.domain;

import java.util.UUID;

public record TenantMember(
        UUID userId,
        String email,
        String fullName,
        String status,
        UUID membershipId,
        boolean defaultMembership) {
}
