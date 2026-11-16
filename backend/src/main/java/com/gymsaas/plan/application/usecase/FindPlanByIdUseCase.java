package com.gymsaas.plan.application.usecase;

import com.gymsaas.plan.application.PlanDetailLoader;
import com.gymsaas.plan.application.PlanDtos;
import com.gymsaas.plan.application.PlanGuard;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
public class FindPlanByIdUseCase {

    private final PlanGuard guard;
    private final PlanDetailLoader detailLoader;

    public FindPlanByIdUseCase(PlanGuard guard, PlanDetailLoader detailLoader) {
        this.guard = guard;
        this.detailLoader = detailLoader;
    }

    @Transactional(readOnly = true)
    public PlanDtos.PlanDetail execute(UUID tenantId, UUID planId) {
        return detailLoader.load(tenantId, guard.requirePlan(tenantId, planId));
    }
}
