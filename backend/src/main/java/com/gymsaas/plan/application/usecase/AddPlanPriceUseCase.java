package com.gymsaas.plan.application.usecase;

import com.gymsaas.audit.application.AuditService;
import com.gymsaas.plan.application.PlanAssembler;
import com.gymsaas.plan.application.PlanDtos;
import com.gymsaas.plan.application.PlanGuard;
import com.gymsaas.plan.application.PlanPriceFactory;
import com.gymsaas.plan.domain.Plan;
import com.gymsaas.plan.domain.PlanPrice;
import com.gymsaas.plan.domain.PlanPriceRepository;
import com.gymsaas.plan.domain.PlanPriceStatus;
import com.gymsaas.plan.domain.PlanStatus;
import com.gymsaas.shared.application.ApiException;
import com.gymsaas.shared.infrastructure.tenant.TenantContext;
import com.gymsaas.tenant.application.TenantCalendar;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Schedules a new price. Prices are never edited: the price covering the new
 * start date is closed the day before and the new price becomes open-ended.
 * Retroactive prices are rejected so the history stays truthful. Existing
 * memberships keep their contracted snapshot.
 */
@Service
public class AddPlanPriceUseCase {

    private final PlanGuard guard;
    private final PlanPriceRepository prices;
    private final PlanPriceFactory priceFactory;
    private final TenantCalendar calendar;
    private final AuditService audit;

    public AddPlanPriceUseCase(PlanGuard guard, PlanPriceRepository prices, PlanPriceFactory priceFactory,
                               TenantCalendar calendar, AuditService audit) {
        this.guard = guard;
        this.prices = prices;
        this.priceFactory = priceFactory;
        this.calendar = calendar;
        this.audit = audit;
    }

    @Transactional
    public PlanDtos.PriceView execute(UUID tenantId, UUID planId, PlanDtos.PriceRequest request) {
        // Row lock serializes concurrent schedule changes of the same plan.
        Plan plan = guard.requirePlanForUpdate(tenantId, planId);
        if (plan.status() == PlanStatus.ARCHIVED) {
            throw ApiException.conflict("PLAN_ARCHIVED", "Plano arquivado não aceita novos preços.");
        }
        LocalDate today = calendar.today(tenantId);
        LocalDate validFrom = request.validFrom() == null ? today : request.validFrom();
        if (validFrom.isBefore(today)) {
            throw ApiException.badRequest("PRICE_RETROACTIVE",
                    "O início de vigência do preço não pode ser anterior a hoje.");
        }

        List<PlanPrice> history = prices.findByPlan(tenantId, planId);
        boolean scheduledConflict = history.stream()
                .anyMatch(price -> price.status() == PlanPriceStatus.ACTIVE && !price.validFrom().isBefore(validFrom));
        if (scheduledConflict) {
            throw ApiException.conflict("SCHEDULED_PRICE_EXISTS",
                    "Já existe preço com vigência a partir desta data. Cancele o preço programado antes.");
        }

        Instant now = calendar.now();
        PlanPrice newPrice = priceFactory.build(plan, request, validFrom, TenantContext.userId(), now);

        PlanPrice previous = history.stream()
                .filter(price -> price.status() == PlanPriceStatus.ACTIVE && price.effectiveOn(validFrom))
                .findFirst().orElse(null);
        if (previous != null) {
            prices.save(previous.closedAt(validFrom.minusDays(1)));
        }
        PlanPrice saved = prices.save(newPrice);

        Map<String, Object> newValues = new HashMap<>();
        newValues.put("amount", saved.amount().toPlainString());
        newValues.put("setupFee", saved.setupFee().toPlainString());
        newValues.put("billingPeriod", saved.billingPeriod().name());
        newValues.put("billingInterval", saved.billingInterval());
        newValues.put("validFrom", saved.validFrom().toString());
        Map<String, Object> oldValues = previous == null ? null : Map.of(
                "priceId", previous.id().toString(),
                "amount", previous.amount().toPlainString(),
                "validUntil", validFrom.minusDays(1).toString());
        audit.recordForTenant(tenantId, "PLAN_PRICE_ADDED", "plan", planId, "HIGH", oldValues, newValues,
                Map.of("priceId", saved.id().toString()));
        return PlanAssembler.price(saved, today);
    }
}
