package com.gymsaas.rbac.infrastructure.persistence;

import com.gymsaas.rbac.domain.Role;
import com.gymsaas.rbac.domain.RoleRepository;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public class RoleRepositoryAdapter implements RoleRepository {

    private final RoleJpaRepository repository;

    public RoleRepositoryAdapter(RoleJpaRepository repository) {
        this.repository = repository;
    }

    @Override
    public List<Role> findAssignableForTenant(UUID tenantId) {
        return repository.findAssignableForTenant(tenantId).stream().map(this::toDomain).toList();
    }

    @Override
    public Optional<Role> findById(UUID id) {
        return repository.findById(id).map(this::toDomain);
    }

    @Override
    public Optional<Role> findSystemByCode(String code) {
        return repository.findByCodeAndTenantIdIsNull(code).map(this::toDomain);
    }

    @Override
    public List<String> findPermissionCodesByRoleCode(String code) {
        return repository.findPermissionCodesByRoleCode(code);
    }

    @Override
    public List<Role> findByIds(Collection<UUID> ids) {
        if (ids == null || ids.isEmpty()) {
            return List.of();
        }
        return repository.findWithPermissionsByIdIn(ids).stream().map(this::toDomain).toList();
    }

    @Override
    public List<String> findPermissionCodesByRoleIds(Collection<UUID> roleIds) {
        if (roleIds == null || roleIds.isEmpty()) {
            return List.of();
        }
        return repository.findPermissionCodesByRoleIds(roleIds);
    }

    private Role toDomain(RoleJpaEntity entity) {
        return new Role(entity.getId(), entity.getTenantId(), entity.getCode(), entity.getName(),
                entity.getDescription(), entity.isSystem());
    }
}
