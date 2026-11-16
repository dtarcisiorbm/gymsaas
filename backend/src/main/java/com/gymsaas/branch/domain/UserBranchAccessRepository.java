package com.gymsaas.branch.domain;

import java.util.List;
import java.util.Set;
import java.util.UUID;

public interface UserBranchAccessRepository {

    List<UUID> findBranchIds(UUID tenantId, UUID userTenantId);

    void replaceAccess(UUID tenantId, UUID userTenantId, Set<UUID> branchIds, UUID grantedBy);
}
