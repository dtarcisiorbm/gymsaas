package com.gymsaas.plan.domain;

import com.gymsaas.shared.domain.PageResult;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface PlanRepository {

    Optional<Plan> findById(UUID tenantId, UUID planId);

    /** Loads the plan with a row lock; serializes price schedule changes. */
    Optional<Plan> findByIdForUpdate(UUID tenantId, UUID planId);

    List<Plan> findAllByIds(UUID tenantId, Collection<UUID> planIds);

    PageResult<Plan> search(UUID tenantId, PlanSearchCriteria criteria);

    Plan save(Plan plan);

    boolean existsByCode(UUID tenantId, String code, UUID excludeId);
}
