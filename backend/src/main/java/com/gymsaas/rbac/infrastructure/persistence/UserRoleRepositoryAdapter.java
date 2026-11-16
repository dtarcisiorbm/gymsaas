package com.gymsaas.rbac.infrastructure.persistence;

import com.gymsaas.rbac.domain.UserRoleRepository;
import com.gymsaas.shared.domain.UuidV7;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Repository
public class UserRoleRepositoryAdapter implements UserRoleRepository {

    private final UserRoleJpaRepository repository;

    public UserRoleRepositoryAdapter(UserRoleJpaRepository repository) {
        this.repository = repository;
    }

    @Override
    public List<UUID> findRoleIds(UUID userTenantId) {
        return repository.findByUserTenantId(userTenantId).stream()
                .map(UserRoleJpaEntity::getRoleId)
                .toList();
    }

    @Override
    @Transactional
    public void replaceRoles(UUID userTenantId, UUID tenantId, Set<UUID> roleIds, UUID assignedBy) {
        repository.deleteByUserTenantId(userTenantId);
        repository.flush();
        Instant now = Instant.now();
        List<UserRoleJpaEntity> entities = roleIds.stream()
                .map(roleId -> new UserRoleJpaEntity(UuidV7.generate(), userTenantId, tenantId, roleId,
                        assignedBy, now))
                .toList();
        repository.saveAll(entities);
    }
}
