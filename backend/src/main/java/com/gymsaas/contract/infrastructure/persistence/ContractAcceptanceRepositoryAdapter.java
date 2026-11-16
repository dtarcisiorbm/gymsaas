package com.gymsaas.contract.infrastructure.persistence;

import com.gymsaas.contract.domain.AcceptanceChannel;
import com.gymsaas.contract.domain.ContractAcceptance;
import com.gymsaas.contract.domain.ContractAcceptanceRepository;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public class ContractAcceptanceRepositoryAdapter implements ContractAcceptanceRepository {

    private final ContractAcceptanceJpaRepository repository;

    public ContractAcceptanceRepositoryAdapter(ContractAcceptanceJpaRepository repository) {
        this.repository = repository;
    }

    @Override
    public Optional<ContractAcceptance> findByContract(UUID tenantId, UUID contractId) {
        return repository.findByTenantIdAndContractId(tenantId, contractId)
                .map(ContractAcceptanceRepositoryAdapter::toDomain);
    }

    @Override
    public List<ContractAcceptance> findByContracts(UUID tenantId, Collection<UUID> contractIds) {
        if (contractIds == null || contractIds.isEmpty()) {
            return List.of();
        }
        return repository.findByTenantIdAndContractIdIn(tenantId, contractIds).stream()
                .map(ContractAcceptanceRepositoryAdapter::toDomain).toList();
    }

    @Override
    public ContractAcceptance save(ContractAcceptance acceptance) {
        ContractAcceptanceJpaEntity entity = new ContractAcceptanceJpaEntity();
        entity.id = acceptance.id();
        entity.tenantId = acceptance.tenantId();
        entity.contractId = acceptance.contractId();
        entity.membershipId = acceptance.membershipId();
        entity.studentId = acceptance.studentId();
        entity.acceptedAt = acceptance.acceptedAt();
        entity.channel = acceptance.channel().name();
        entity.acceptorName = acceptance.acceptorName();
        entity.acceptorDocumentMasked = acceptance.acceptorDocumentMasked();
        entity.contentSha256 = acceptance.contentSha256();
        entity.declaration = acceptance.declaration();
        entity.ipAddress = acceptance.ipAddress();
        entity.userAgent = acceptance.userAgent();
        entity.registeredBy = acceptance.registeredBy();
        entity.createdAt = acceptance.createdAt();
        return toDomain(repository.saveAndFlush(entity));
    }

    static ContractAcceptance toDomain(ContractAcceptanceJpaEntity entity) {
        return new ContractAcceptance(entity.id, entity.tenantId, entity.contractId, entity.membershipId,
                entity.studentId, entity.acceptedAt, AcceptanceChannel.valueOf(entity.channel), entity.acceptorName,
                entity.acceptorDocumentMasked, entity.contentSha256.trim(), entity.declaration, entity.ipAddress,
                entity.userAgent, entity.registeredBy, entity.createdAt);
    }
}
