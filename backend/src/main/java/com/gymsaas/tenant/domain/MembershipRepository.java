package com.gymsaas.tenant.domain;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface MembershipRepository {

    List<Membership> findActiveByUserId(UUID userId);

    Optional<Membership> findActiveByUserAndTenant(UUID userId, UUID tenantId);

    Optional<Membership> findById(UUID id);

    Optional<Membership> findByUserAndTenantIncludingInactive(UUID userId, UUID tenantId);

    Membership create(UUID id, UUID userId, UUID tenantId, boolean defaultMembership);
}
