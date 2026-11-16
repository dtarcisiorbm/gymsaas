package com.gymsaas.contract.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ContractAcceptanceJpaRepository extends JpaRepository<ContractAcceptanceJpaEntity, UUID> {

    Optional<ContractAcceptanceJpaEntity> findByTenantIdAndContractId(UUID tenantId, UUID contractId);

    List<ContractAcceptanceJpaEntity> findByTenantIdAndContractIdIn(UUID tenantId, Collection<UUID> contractIds);
}
