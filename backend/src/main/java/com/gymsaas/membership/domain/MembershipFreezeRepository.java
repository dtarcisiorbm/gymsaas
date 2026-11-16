package com.gymsaas.membership.domain;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface MembershipFreezeRepository {

    List<MembershipFreeze> findByMembership(UUID tenantId, UUID membershipId);

    Optional<MembershipFreeze> findActive(UUID tenantId, UUID membershipId);

    /** Active freezes whose planned end is before {@code date} (to be completed). */
    List<MembershipFreeze> findActiveEndingBefore(UUID tenantId, LocalDate date);

    MembershipFreeze save(MembershipFreeze freeze);
}
