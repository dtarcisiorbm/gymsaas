package com.gymsaas.plan.domain;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface PlanPriceRepository {

    /** Full history, newest validity first. */
    List<PlanPrice> findByPlan(UUID tenantId, UUID planId);

    Optional<PlanPrice> findById(UUID tenantId, UUID priceId);

    Optional<PlanPrice> findEffective(UUID tenantId, UUID planId, LocalDate date);

    List<PlanPrice> findEffectiveForPlans(UUID tenantId, Collection<UUID> planIds, LocalDate date);

    PlanPrice save(PlanPrice price);
}
