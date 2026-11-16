package com.gymsaas.plan.application.usecase;

import com.gymsaas.audit.application.AuditService;
import com.gymsaas.plan.application.PlanDetailLoader;
import com.gymsaas.plan.application.PlanDtos;
import com.gymsaas.plan.application.PlanGuard;
import com.gymsaas.plan.domain.DurationType;
import com.gymsaas.plan.domain.FreezePolicy;
import com.gymsaas.plan.domain.Plan;
import com.gymsaas.plan.domain.PlanPrice;
import com.gymsaas.plan.domain.PlanPriceRepository;
import com.gymsaas.plan.domain.PlanPriceStatus;
import com.gymsaas.plan.domain.PlanRepository;
import com.gymsaas.shared.application.ApiException;
import com.gymsaas.tenant.application.TenantCalendar;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.Map;
import java.util.UUID;

/**
 * Updates plan definition. Existing memberships are not affected: their
 * commercial conditions and duration are snapshotted at contracting time.
 */
@Service
public class UpdatePlanUseCase {

    private final PlanGuard guard;
    private final PlanRepository plans;
    private final PlanPriceRepository prices;
    private final PlanDetailLoader detailLoader;
    private final TenantCalendar calendar;
    private final AuditService audit;

    public UpdatePlanUseCase(PlanGuard guard, PlanRepository plans, PlanPriceRepository prices,
                             PlanDetailLoader detailLoader, TenantCalendar calendar, AuditService audit) {
        this.guard = guard;
        this.plans = plans;
        this.prices = prices;
        this.detailLoader = detailLoader;
        this.calendar = calendar;
        this.audit = audit;
    }

    @Transactional
    public PlanDtos.PlanDetail execute(UUID tenantId, UUID planId, PlanDtos.UpdatePlanRequest request) {
        Plan current = guard.requirePlanForUpdate(tenantId, planId);
        if (request.version() != null && request.version() != current.version()) {
            throw ApiException.conflict("CONCURRENT_UPDATE",
                    "O plano foi alterado por outra pessoa. Recarregue e tente novamente.");
        }
        DurationType durationType = guard.duration(request.durationType(), request.durationValue());
        int durationValue = PlanGuard.durationValue(request.durationValue());
        FreezePolicy freezePolicy = guard.freezePolicy(request.freezePolicy());

        LocalDate today = calendar.today(tenantId);
        for (PlanPrice price : prices.findByPlan(tenantId, planId)) {
            boolean currentOrFuture = price.status() == PlanPriceStatus.ACTIVE
                    && (price.validUntil() == null || !price.validUntil().isBefore(today));
            if (currentOrFuture && !price.billingPeriod()
                    .compatibleWith(durationType, durationValue, price.billingInterval())) {
                throw ApiException.conflict("PRICE_INCOMPATIBLE_WITH_DURATION",
                        "Há preço vigente ou programado com periodicidade incompatível com a nova duração.");
            }
        }

        Plan updated = new Plan(current.id(), current.tenantId(), current.code(), request.name().trim(),
                PlanGuard.trimToNull(request.description()), durationType, durationValue, current.status(),
                freezePolicy, PlanGuard.trimToNull(request.contractTerms()), current.version(),
                current.createdBy(), current.createdAt(), calendar.now());
        Plan saved = plans.save(updated);

        audit.recordChange("PLAN_UPDATED", "plan", saved.id(), "MEDIUM",
                Map.of("name", current.name(), "durationType", current.durationType().name(),
                        "durationValue", current.durationValue()),
                Map.of("name", saved.name(), "durationType", saved.durationType().name(),
                        "durationValue", saved.durationValue()));
        return detailLoader.load(tenantId, saved);
    }
}
