package com.gymsaas.branch.domain;

import com.gymsaas.shared.domain.PageResult;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface BranchRepository {

    Optional<Branch> findById(UUID tenantId, UUID branchId);

    List<Branch> findAllByIds(UUID tenantId, Collection<UUID> branchIds);

    PageResult<Branch> search(UUID tenantId, String status, String search, int page, int size);

    Branch save(Branch branch);

    boolean existsByCode(UUID tenantId, String code, UUID excludeId);
}
