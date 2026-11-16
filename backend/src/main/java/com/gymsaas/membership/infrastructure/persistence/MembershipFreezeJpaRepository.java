package com.gymsaas.membership.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface MembershipFreezeJpaRepository extends JpaRepository<MembershipFreezeJpaEntity, UUID> {

    List<MembershipFreezeJpaEntity> findByTenantIdAndMembershipIdOrderByStartDateDescCreatedAtDesc(UUID tenantId,
                                                                                                   UUID membershipId);

    Optional<MembershipFreezeJpaEntity> findFirstByTenantIdAndMembershipIdAndStatus(UUID tenantId, UUID membershipId,
                                                                                    String status);

    List<MembershipFreezeJpaEntity> findByTenantIdAndStatusAndPlannedEndDateLessThan(UUID tenantId, String status,
                                                                                    LocalDate date);
}
