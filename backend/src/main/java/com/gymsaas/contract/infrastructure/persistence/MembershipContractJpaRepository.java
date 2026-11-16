package com.gymsaas.contract.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface MembershipContractJpaRepository extends JpaRepository<MembershipContractJpaEntity, UUID> {

    Optional<MembershipContractJpaEntity> findByIdAndTenantId(UUID id, UUID tenantId);

    List<MembershipContractJpaEntity> findByTenantIdAndMembershipIdOrderByContractVersionDesc(UUID tenantId,
                                                                                            UUID membershipId);

    Optional<MembershipContractJpaEntity> findFirstByTenantIdAndMembershipIdAndStatusIn(UUID tenantId,
                                                                                        UUID membershipId,
                                                                                        Collection<String> statuses);
}
