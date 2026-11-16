package com.gymsaas.branch.infrastructure.persistence;

import com.gymsaas.branch.domain.Branch;
import com.gymsaas.branch.domain.BranchRepository;
import com.gymsaas.shared.domain.PageResult;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public class BranchRepositoryAdapter implements BranchRepository {

    private final BranchJpaRepository repository;

    public BranchRepositoryAdapter(BranchJpaRepository repository) {
        this.repository = repository;
    }

    @Override
    public Optional<Branch> findById(UUID tenantId, UUID branchId) {
        return repository.findByIdAndTenantId(branchId, tenantId).map(this::toDomain);
    }

    @Override
    public List<Branch> findAllByIds(UUID tenantId, Collection<UUID> branchIds) {
        if (branchIds == null || branchIds.isEmpty()) {
            return List.of();
        }
        return repository.findByTenantIdAndIdIn(tenantId, branchIds).stream()
                .map(this::toDomain)
                .toList();
    }

    @Override
    public PageResult<Branch> search(UUID tenantId, String status, String search, int page, int size) {
        Page<BranchJpaEntity> result = repository.search(tenantId, emptyIfBlank(status),
                emptyIfBlank(search), PageRequest.of(page, size));
        return PageResult.of(result.getContent().stream().map(this::toDomain).toList(),
                result.getTotalElements(), page, size);
    }

    @Override
    public Branch save(Branch branch) {
        BranchJpaEntity entity = repository.findById(branch.id())
                .orElseGet(() -> new BranchJpaEntity(branch.id(), branch.tenantId(), branch.name(),
                        branch.code(), branch.document(), branch.email(), branch.phone(),
                        branch.zipCode(), branch.street(), branch.number(), branch.complement(),
                        branch.neighborhood(), branch.city(), branch.state(), branch.status(),
                        branch.createdAt(), branch.updatedAt()));
        entity.setName(branch.name());
        entity.setCode(branch.code());
        entity.setDocument(branch.document());
        entity.setEmail(branch.email());
        entity.setPhone(branch.phone());
        entity.setZipCode(branch.zipCode());
        entity.setStreet(branch.street());
        entity.setAddressNumber(branch.number());
        entity.setComplement(branch.complement());
        entity.setNeighborhood(branch.neighborhood());
        entity.setCity(branch.city());
        entity.setState(branch.state());
        entity.setStatus(branch.status());
        return toDomain(repository.save(entity));
    }

    @Override
    public boolean existsByCode(UUID tenantId, String code, UUID excludeId) {
        return repository.findByTenantIdAndCodeIgnoreCase(tenantId, code)
                .filter(entity -> excludeId == null || !entity.getId().equals(excludeId))
                .isPresent();
    }

    private String emptyIfBlank(String value) {
        return value == null ? "" : value.trim();
    }

    private Branch toDomain(BranchJpaEntity entity) {
        return new Branch(entity.getId(), entity.getTenantId(), entity.getName(), entity.getCode(),
                entity.getDocument(), entity.getEmail(), entity.getPhone(), entity.getZipCode(),
                entity.getStreet(), entity.getAddressNumber(), entity.getComplement(),
                entity.getNeighborhood(), entity.getCity(), entity.getState(), entity.getStatus(),
                entity.getCreatedAt(), entity.getUpdatedAt());
    }
}
