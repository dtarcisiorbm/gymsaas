package com.gymsaas.plan.application.usecase;

import com.gymsaas.audit.application.AuditService;
import com.gymsaas.plan.application.PlanDetailLoader;
import com.gymsaas.plan.application.PlanDtos;
import com.gymsaas.plan.application.PlanGuard;
import com.gymsaas.plan.application.PlanPriceFactory;
import com.gymsaas.plan.domain.DurationType;
import com.gymsaas.plan.domain.FreezePolicy;
import com.gymsaas.plan.domain.Plan;
import com.gymsaas.plan.domain.PlanPrice;
import com.gymsaas.plan.domain.PlanPriceRepository;
import com.gymsaas.plan.domain.PlanRepository;
import com.gymsaas.plan.domain.PlanStatus;
import com.gymsaas.shared.application.ApiException;
import com.gymsaas.shared.domain.UuidV7;
import com.gymsaas.shared.infrastructure.tenant.TenantContext;
import com.gymsaas.tenant.application.TenantCalendar;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Service
public class CreatePlanUseCase {

    private final PlanGuard guard;
    private final PlanRepository plans;
    private final PlanPriceRepository prices;
    private final PlanPriceFactory priceFactory;
    private final PlanDetailLoader detailLoader;
    private final TenantCalendar calendar;
    private final AuditService audit;

    public CreatePlanUseCase(PlanGuard guard, PlanRepository plans, PlanPriceRepository prices,
                             PlanPriceFactory priceFactory, PlanDetailLoader detailLoader,
                             TenantCalendar calendar, AuditService audit) {
        this.guard = guard;
        this.plans = plans;
        this.prices = prices;
        this.priceFactory = priceFactory;
        this.detailLoader = detailLoader;
        this.calendar = calendar;
        this.audit = audit;
    }

    @Transactional
    public PlanDtos.PlanDetail execute(UUID tenantId, PlanDtos.CreatePlanRequest request) {
        String code = request.code().trim().toUpperCase();
        if (plans.existsByCode(tenantId, code, null)) {
            throw ApiException.conflict("PLAN_CODE_EXISTS", "Já existe um plano com este código.");
        }
        DurationType durationType = guard.duration(request.durationType(), request.durationValue());
        int durationValue = PlanGuard.durationValue(request.durationValue());
        FreezePolicy freezePolicy = guard.freezePolicy(request.freezePolicy());

        Instant now = calendar.now();
        UUID actor = TenantContext.userId();
        Plan saved = plans.save(new Plan(UuidV7.generate(), tenantId, code, request.name().trim(),
                PlanGuard.trimToNull(request.description()), durationType, durationValue, PlanStatus.ACTIVE,
                freezePolicy, PlanGuard.trimToNull(request.contractTerms()), 0, actor, now, now));

        Map<String, Object> values = new HashMap<>();
        values.put("code", saved.code());
        values.put("name", saved.name());
        values.put("durationType", saved.durationType().name());
        values.put("durationValue", saved.durationValue());
        if (request.initialPrice() != null) {
            LocalDate validFrom = request.initialPrice().validFrom() == null
                    ? calendar.today(tenantId) : request.initialPrice().validFrom();
            PlanPrice price = prices.save(priceFactory.build(saved, request.initialPrice(), validFrom, actor, now));
            values.put("initialPrice", price.amount().toPlainString());
            values.put("billingPeriod", price.billingPeriod().name());
        }
        audit.recordForTenant(tenantId, "PLAN_CREATED", "plan", saved.id(), "MEDIUM", null, values, Map.of());
        return detailLoader.load(tenantId, saved);
    }
}
