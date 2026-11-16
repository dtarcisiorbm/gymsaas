package com.gymsaas.membership.domain;

import com.gymsaas.shared.domain.PageResult;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface MembershipRepository {

    Optional<Membership> findById(UUID tenantId, UUID membershipId);

    List<Membership> findAllByIds(UUID tenantId, Collection<UUID> membershipIds);

    PageResult<Membership> search(UUID tenantId, MembershipSearchCriteria criteria);

    /**
     * Inserts or updates. Updates are optimistic: the stored version must equal
     * {@code membership.version()}, otherwise a concurrency failure is raised.
     */
    Membership save(Membership membership);

    boolean existsOpenOverlap(UUID tenantId, UUID studentId, UUID planId, LocalDate start, LocalDate end,
                              UUID excludeId);

    Optional<Membership> findLiveRenewalOf(UUID tenantId, UUID membershipId);

    List<Membership> findPendingStartingOnOrBefore(UUID tenantId, LocalDate date);

    /** ACTIVE or SUSPENDED memberships whose end date is before {@code date}. */
    List<Membership> findRunningEndedBefore(UUID tenantId, LocalDate date);
}
