package com.gymsaas.membership.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface MembershipHistoryJpaRepository extends JpaRepository<MembershipHistoryJpaEntity, UUID> {

    List<MembershipHistoryJpaEntity> findByTenantIdAndMembershipIdOrderByChangedAtDescIdDesc(UUID tenantId,
                                                                                           UUID membershipId);
}
