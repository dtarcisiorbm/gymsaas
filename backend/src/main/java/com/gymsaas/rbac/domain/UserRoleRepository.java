package com.gymsaas.rbac.domain;

import java.util.List;
import java.util.Set;
import java.util.UUID;

public interface UserRoleRepository {

    List<UUID> findRoleIds(UUID userTenantId);

    void replaceRoles(UUID userTenantId, UUID tenantId, Set<UUID> roleIds, UUID assignedBy);
}
