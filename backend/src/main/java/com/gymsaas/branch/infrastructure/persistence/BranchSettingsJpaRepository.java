package com.gymsaas.branch.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface BranchSettingsJpaRepository extends JpaRepository<BranchSettingsJpaEntity, UUID> {

    Optional<BranchSettingsJpaEntity> findByTenantIdAndBranchId(UUID tenantId, UUID branchId);
}
