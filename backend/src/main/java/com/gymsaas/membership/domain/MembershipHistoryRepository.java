package com.gymsaas.membership.domain;

import java.util.List;
import java.util.UUID;

public interface MembershipHistoryRepository {

    void append(MembershipEvent event);

    /** Newest first. */
    List<MembershipEvent> findByMembership(UUID tenantId, UUID membershipId);
}
