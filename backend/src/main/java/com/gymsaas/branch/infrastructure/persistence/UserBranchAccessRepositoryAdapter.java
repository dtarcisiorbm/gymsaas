package com.gymsaas.branch.infrastructure.persistence;

import com.gymsaas.branch.domain.UserBranchAccessRepository;
import com.gymsaas.shared.domain.UuidV7;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Repository
public class UserBranchAccessRepositoryAdapter implements UserBranchAccessRepository {

    private final UserBranchAccessJpaRepository repository;

    public UserBranchAccessRepositoryAdapter(UserBranchAccessJpaRepository repository) {
        this.repository = repository;
    }

    @Override
    public List<UUID> findBranchIds(UUID tenantId, UUID userTenantId) {
        return repository.findByTenantIdAndUserTenantId(tenantId, userTenantId).stream()
                .map(UserBranchAccessJpaEntity::getBranchId)
                .toList();
    }

    @Override
    @Transactional
    public void replaceAccess(UUID tenantId, UUID userTenantId, Set<UUID> branchIds, UUID grantedBy) {
        repository.deleteByTenantIdAndUserTenantId(tenantId, userTenantId);
        repository.flush();
        Instant now = Instant.now();
        List<UserBranchAccessJpaEntity> entities = branchIds.stream()
                .map(branchId -> new UserBranchAccessJpaEntity(UuidV7.generate(), tenantId,
                        userTenantId, branchId, grantedBy, now))
                .toList();
        repository.saveAll(entities);
    }
}
