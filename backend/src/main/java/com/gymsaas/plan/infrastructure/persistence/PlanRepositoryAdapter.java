package com.gymsaas.plan.infrastructure.persistence;

import com.gymsaas.plan.domain.DurationType;
import com.gymsaas.plan.domain.FreezePolicy;
import com.gymsaas.plan.domain.Plan;
import com.gymsaas.plan.domain.PlanRepository;
import com.gymsaas.plan.domain.PlanSearchCriteria;
import com.gymsaas.plan.domain.PlanStatus;
import com.gymsaas.shared.domain.PageResult;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

@Repository
public class PlanRepositoryAdapter implements PlanRepository {

    private static final Set<String> ALLOWED_SORTS = Set.of("name", "code", "createdAt", "status");

    private final PlanJpaRepository repository;

    public PlanRepositoryAdapter(PlanJpaRepository repository) {
        this.repository = repository;
    }

    @Override
    public Optional<Plan> findById(UUID tenantId, UUID planId) {
        return repository.findByIdAndTenantId(planId, tenantId).map(PlanRepositoryAdapter::toDomain);
    }

    @Override
    public Optional<Plan> findByIdForUpdate(UUID tenantId, UUID planId) {
        return repository.findForUpdate(tenantId, planId).map(PlanRepositoryAdapter::toDomain);
    }

    @Override
    public List<Plan> findAllByIds(UUID tenantId, Collection<UUID> planIds) {
        if (planIds == null || planIds.isEmpty()) {
            return List.of();
        }
        return repository.findByTenantIdAndIdIn(tenantId, planIds).stream()
                .map(PlanRepositoryAdapter::toDomain).toList();
    }

    @Override
    public PageResult<Plan> search(UUID tenantId, PlanSearchCriteria criteria) {
        String sort = criteria.sort() != null && ALLOWED_SORTS.contains(criteria.sort()) ? criteria.sort() : "name";
        Sort.Direction direction = "desc".equalsIgnoreCase(criteria.direction())
                ? Sort.Direction.DESC : Sort.Direction.ASC;
        Page<PlanJpaEntity> result = repository.search(tenantId,
                criteria.status() == null ? "" : criteria.status().name(),
                criteria.search() == null ? "" : criteria.search().trim(),
                PageRequest.of(criteria.page(), criteria.size(), Sort.by(direction, sort)));
        return PageResult.of(result.getContent().stream().map(PlanRepositoryAdapter::toDomain).toList(),
                result.getTotalElements(), criteria.page(), criteria.size());
    }

    @Override
    public Plan save(Plan plan) {
        PlanJpaEntity entity = repository.findById(plan.id())
                .orElseGet(() -> new PlanJpaEntity(plan.id(), plan.tenantId(), plan.code(), plan.createdBy(),
                        plan.createdAt()));
        entity.name = plan.name();
        entity.description = plan.description();
        entity.durationType = plan.durationType().name();
        entity.durationValue = plan.durationValue();
        entity.status = plan.status().name();
        FreezePolicy policy = plan.freezePolicy() == null ? FreezePolicy.unlimited() : plan.freezePolicy();
        entity.maxFreezeDays = policy.maxFreezeDays();
        entity.maxFreezes = policy.maxFreezes();
        entity.minFreezeDays = policy.minFreezeDays();
        entity.contractTerms = plan.contractTerms();
        entity.updatedAt = plan.updatedAt();
        return toDomain(repository.saveAndFlush(entity));
    }

    @Override
    public boolean existsByCode(UUID tenantId, String code, UUID excludeId) {
        return excludeId == null
                ? repository.existsByTenantIdAndCode(tenantId, code)
                : repository.existsByTenantIdAndCodeAndIdNot(tenantId, code, excludeId);
    }

    static Plan toDomain(PlanJpaEntity entity) {
        return new Plan(entity.id, entity.tenantId, entity.code, entity.name, entity.description,
                DurationType.parse(entity.durationType), entity.durationValue, PlanStatus.parse(entity.status),
                new FreezePolicy(entity.maxFreezeDays, entity.maxFreezes, entity.minFreezeDays),
                entity.contractTerms, entity.version, entity.createdBy, entity.createdAt, entity.updatedAt);
    }
}
