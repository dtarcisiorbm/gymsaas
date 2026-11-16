package com.gymsaas.contract.domain;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ContractAcceptanceRepository {

    Optional<ContractAcceptance> findByContract(UUID tenantId, UUID contractId);

    List<ContractAcceptance> findByContracts(UUID tenantId, Collection<UUID> contractIds);

    ContractAcceptance save(ContractAcceptance acceptance);
}
