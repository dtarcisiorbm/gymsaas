package com.gymsaas.plan.domain;

import java.util.List;
import java.util.UUID;

public interface PlanFeatureRepository {

    List<PlanFeature> findByPlan(UUID tenantId, UUID planId);

    /** Replaces the plan's feature set atomically (inside the caller's transaction). */
    List<PlanFeature> replace(UUID tenantId, UUID planId, List<PlanFeature> features);
}
