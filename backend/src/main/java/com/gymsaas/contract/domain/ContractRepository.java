package com.gymsaas.contract.domain;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ContractRepository {

    Optional<MembershipContract> findById(UUID tenantId, UUID contractId);

    /** Newest version first. */
    List<MembershipContract> findByMembership(UUID tenantId, UUID membershipId);

    /** GENERATED or ACCEPTED contract of the membership, if any. */
    Optional<MembershipContract> findCurrent(UUID tenantId, UUID membershipId);

    MembershipContract save(MembershipContract contract);
}
