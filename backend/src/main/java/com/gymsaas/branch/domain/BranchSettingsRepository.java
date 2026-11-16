package com.gymsaas.branch.domain;

import java.util.Optional;
import java.util.UUID;

public interface BranchSettingsRepository {

    Optional<BranchSettings> find(UUID tenantId, UUID branchId);

    BranchSettings save(BranchSettings settings);
}
