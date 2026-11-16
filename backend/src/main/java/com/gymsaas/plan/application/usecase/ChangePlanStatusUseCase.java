package com.gymsaas.plan.application.usecase;

import com.gymsaas.audit.application.AuditService;
import com.gymsaas.plan.application.PlanDetailLoader;
import com.gymsaas.plan.application.PlanDtos;
import com.gymsaas.plan.application.PlanGuard;
import com.gymsaas.plan.domain.Plan;
import com.gymsaas.plan.domain.PlanRepository;
import com.gymsaas.plan.domain.PlanStatus;
import com.gymsaas.shared.application.ApiException;
import com.gymsaas.tenant.application.TenantCalendar;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;
import java.util.UUID;

/** Deactivating/archiving a plan blocks new sales only; live memberships keep running. */
@Service
public class ChangePlanStatusUseCase {

    private final PlanGuard guard;
    private final PlanRepository plans;
    private final PlanDetailLoader detailLoader;
    private final TenantCalendar calendar;
    private final AuditService audit;

    public ChangePlanStatusUseCase(PlanGuard guard, PlanRepository plans, PlanDetailLoader detailLoader,
                                   TenantCalendar calendar, AuditService audit) {
        this.guard = guard;
        this.plans = plans;
        this.detailLoader = detailLoader;
        this.calendar = calendar;
        this.audit = audit;
    }

    @Transactional
    public PlanDtos.PlanDetail execute(UUID tenantId, UUID planId, PlanDtos.ChangePlanStatusRequest request) {
        Plan current = guard.requirePlanForUpdate(tenantId, planId);
        PlanStatus target = guard.status(request.status());
        if (!current.status().canTransitionTo(target)) {
            throw ApiException.conflict("INVALID_STATUS_TRANSITION", "Transição de status não permitida.");
        }
        Plan saved = plans.save(new Plan(current.id(), current.tenantId(), current.code(), current.name(),
                current.description(), current.durationType(), current.durationValue(), target,
                current.freezePolicy(), current.contractTerms(), current.version(), current.createdBy(),
                current.createdAt(), calendar.now()));
        audit.recordChange("PLAN_STATUS_CHANGED", "plan", saved.id(), "MEDIUM",
                Map.of("status", current.status().name()), Map.of("status", target.name()));
        return detailLoader.load(tenantId, saved);
    }
}
