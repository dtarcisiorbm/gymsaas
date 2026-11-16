package com.gymsaas.rbac.domain;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface RoleRepository {

    List<Role> findAssignableForTenant(UUID tenantId);

    Optional<Role> findById(UUID id);

    Optional<Role> findSystemByCode(String code);

    List<String> findPermissionCodesByRoleCode(String code);

    List<Role> findByIds(Collection<UUID> ids);

    List<String> findPermissionCodesByRoleIds(Collection<UUID> roleIds);
}
