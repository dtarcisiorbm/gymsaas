package com.gymsaas.contract.infrastructure.persistence;

import com.gymsaas.contract.domain.ContractRepository;
import com.gymsaas.contract.domain.ContractStatus;
import com.gymsaas.contract.domain.MembershipContract;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public class ContractRepositoryAdapter implements ContractRepository {

    private final MembershipContractJpaRepository repository;

    public ContractRepositoryAdapter(MembershipContractJpaRepository repository) {
        this.repository = repository;
    }

    @Override
    public Optional<MembershipContract> findById(UUID tenantId, UUID contractId) {
        return repository.findByIdAndTenantId(contractId, tenantId).map(ContractRepositoryAdapter::toDomain);
    }

    @Override
    public List<MembershipContract> findByMembership(UUID tenantId, UUID membershipId) {
        return repository.findByTenantIdAndMembershipIdOrderByContractVersionDesc(tenantId, membershipId).stream()
                .map(ContractRepositoryAdapter::toDomain).toList();
    }

    @Override
    public Optional<MembershipContract> findCurrent(UUID tenantId, UUID membershipId) {
        return repository.findFirstByTenantIdAndMembershipIdAndStatusIn(tenantId, membershipId,
                        List.of(ContractStatus.GENERATED.name(), ContractStatus.ACCEPTED.name()))
                .map(ContractRepositoryAdapter::toDomain);
    }

    /**
     * Content is written once; afterwards only the status changes (V12
     * trigger). Flushes immediately so superseding the previous version
     * reaches the database before the new version is inserted.
     */
    @Override
    public MembershipContract save(MembershipContract contract) {
        MembershipContractJpaEntity entity = repository.findById(contract.id()).orElse(null);
        if (entity == null) {
            entity = new MembershipContractJpaEntity();
            entity.id = contract.id();
            entity.tenantId = contract.tenantId();
            entity.membershipId = contract.membershipId();
            entity.contractVersion = contract.contractVersion();
            entity.templateVersion = contract.templateVersion();
            entity.content = contract.content();
            entity.contentSha256 = contract.contentSha256();
            entity.storageKey = contract.storageKey();
            entity.documentSha256 = contract.documentSha256();
            entity.documentSizeBytes = contract.documentSizeBytes();
            entity.documentContentType = contract.documentContentType();
            entity.generatedBy = contract.generatedBy();
            entity.generatedAt = contract.generatedAt();
        }
        entity.status = contract.status().name();
        entity.statusChangedAt = contract.statusChangedAt();
        return toDomain(repository.saveAndFlush(entity));
    }

    static MembershipContract toDomain(MembershipContractJpaEntity entity) {
        return new MembershipContract(entity.id, entity.tenantId, entity.membershipId, entity.contractVersion,
                entity.templateVersion, ContractStatus.valueOf(entity.status), entity.content,
                entity.contentSha256.trim(), entity.storageKey, entity.documentSha256.trim(),
                entity.documentSizeBytes, entity.documentContentType, entity.generatedBy, entity.generatedAt,
                entity.statusChangedAt);
    }
}
