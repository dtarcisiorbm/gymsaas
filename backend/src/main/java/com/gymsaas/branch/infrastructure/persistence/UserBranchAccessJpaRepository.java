package com.gymsaas.branch.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface UserBranchAccessJpaRepository extends JpaRepository<UserBranchAccessJpaEntity, UUID> {

    List<UserBranchAccessJpaEntity> findByTenantIdAndUserTenantId(UUID tenantId, UUID userTenantId);

    void deleteByTenantIdAndUserTenantId(UUID tenantId, UUID userTenantId);
}
