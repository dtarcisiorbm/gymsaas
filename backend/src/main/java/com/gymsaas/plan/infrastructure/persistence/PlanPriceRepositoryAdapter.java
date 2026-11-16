package com.gymsaas.plan.infrastructure.persistence;

import com.gymsaas.plan.domain.BillingPeriod;
import com.gymsaas.plan.domain.PlanPrice;
import com.gymsaas.plan.domain.PlanPriceRepository;
import com.gymsaas.plan.domain.PlanPriceStatus;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public class PlanPriceRepositoryAdapter implements PlanPriceRepository {

    private final PlanPriceJpaRepository repository;

    public PlanPriceRepositoryAdapter(PlanPriceJpaRepository repository) {
        this.repository = repository;
    }

    @Override
    public List<PlanPrice> findByPlan(UUID tenantId, UUID planId) {
        return repository.findByTenantIdAndPlanIdOrderByValidFromDescCreatedAtDesc(tenantId, planId).stream()
                .map(PlanPriceRepositoryAdapter::toDomain).toList();
    }

    @Override
    public Optional<PlanPrice> findById(UUID tenantId, UUID priceId) {
        return repository.findByIdAndTenantId(priceId, tenantId).map(PlanPriceRepositoryAdapter::toDomain);
    }

    @Override
    public Optional<PlanPrice> findEffective(UUID tenantId, UUID planId, LocalDate date) {
        return repository.findEffective(tenantId, planId, date).map(PlanPriceRepositoryAdapter::toDomain);
    }

    @Override
    public List<PlanPrice> findEffectiveForPlans(UUID tenantId, Collection<UUID> planIds, LocalDate date) {
        if (planIds == null || planIds.isEmpty()) {
            return List.of();
        }
        return repository.findEffectiveIn(tenantId, planIds, date).stream()
                .map(PlanPriceRepositoryAdapter::toDomain).toList();
    }

    /**
     * Inserts a new price or applies the only mutable fields (validity end and
     * status). Flushes immediately so a closing UPDATE reaches the database
     * before the INSERT of its successor (exclusion constraint ordering).
     */
    @Override
    public PlanPrice save(PlanPrice price) {
        PlanPriceJpaEntity entity = repository.findById(price.id()).orElse(null);
        if (entity == null) {
            entity = new PlanPriceJpaEntity();
            entity.id = price.id();
            entity.tenantId = price.tenantId();
            entity.planId = price.planId();
            entity.amount = price.amount();
            entity.currency = price.currency();
            entity.billingPeriod = price.billingPeriod().name();
            entity.billingInterval = price.billingInterval();
            entity.setupFee = price.setupFee();
            entity.validFrom = price.validFrom();
            entity.notes = price.notes();
            entity.createdBy = price.createdBy();
            entity.createdAt = price.createdAt();
        }
        entity.validUntil = price.validUntil();
        entity.status = price.status().name();
        return toDomain(repository.saveAndFlush(entity));
    }

    static PlanPrice toDomain(PlanPriceJpaEntity entity) {
        return new PlanPrice(entity.id, entity.tenantId, entity.planId, entity.amount, entity.currency.trim(),
                BillingPeriod.parse(entity.billingPeriod), entity.billingInterval, entity.setupFee,
                entity.validFrom, entity.validUntil, PlanPriceStatus.valueOf(entity.status), entity.notes,
                entity.createdBy, entity.createdAt);
    }
}
