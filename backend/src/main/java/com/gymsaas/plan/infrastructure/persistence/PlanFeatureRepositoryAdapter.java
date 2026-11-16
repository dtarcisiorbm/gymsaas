package com.gymsaas.plan.infrastructure.persistence;

import com.gymsaas.plan.domain.PlanFeature;
import com.gymsaas.plan.domain.PlanFeatureRepository;
import com.gymsaas.plan.domain.PlanFeatureType;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public class PlanFeatureRepositoryAdapter implements PlanFeatureRepository {

    private final PlanFeatureJpaRepository repository;

    public PlanFeatureRepositoryAdapter(PlanFeatureJpaRepository repository) {
        this.repository = repository;
    }

    @Override
    public List<PlanFeature> findByPlan(UUID tenantId, UUID planId) {
        return repository.findByTenantIdAndPlanIdOrderBySortOrderAsc(tenantId, planId).stream()
                .map(PlanFeatureRepositoryAdapter::toDomain).toList();
    }

    @Override
    public List<PlanFeature> replace(UUID tenantId, UUID planId, List<PlanFeature> features) {
        repository.deleteByPlan(tenantId, planId);
        List<PlanFeatureJpaEntity> entities = features.stream().map(feature -> {
            PlanFeatureJpaEntity entity = new PlanFeatureJpaEntity();
            entity.id = feature.id();
            entity.tenantId = tenantId;
            entity.planId = planId;
            entity.featureType = feature.type().name();
            entity.branchId = feature.branchId();
            entity.intValue = feature.intValue();
            entity.startTime = feature.startTime();
            entity.endTime = feature.endTime();
            entity.daysOfWeek = feature.daysOfWeek() == null ? null : feature.daysOfWeek().shortValue();
            entity.label = feature.label();
            entity.description = feature.description();
            entity.sortOrder = feature.sortOrder();
            entity.createdAt = feature.createdAt();
            return entity;
        }).toList();
        return repository.saveAllAndFlush(entities).stream().map(PlanFeatureRepositoryAdapter::toDomain).toList();
    }

    static PlanFeature toDomain(PlanFeatureJpaEntity entity) {
        return new PlanFeature(entity.id, entity.tenantId, entity.planId, PlanFeatureType.parse(entity.featureType),
                entity.branchId, entity.intValue, entity.startTime, entity.endTime,
                entity.daysOfWeek == null ? null : entity.daysOfWeek.intValue(), entity.label, entity.description,
                entity.sortOrder, entity.createdAt);
    }
}
