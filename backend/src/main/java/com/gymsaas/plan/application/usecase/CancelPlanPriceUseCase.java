package com.gymsaas.plan.application.usecase;

import com.gymsaas.audit.application.AuditService;
import com.gymsaas.plan.application.PlanAssembler;
import com.gymsaas.plan.application.PlanDtos;
import com.gymsaas.plan.application.PlanGuard;
import com.gymsaas.plan.domain.PlanPrice;
import com.gymsaas.plan.domain.PlanPriceRepository;
import com.gymsaas.plan.domain.PlanPriceStatus;
import com.gymsaas.shared.application.ApiException;
import com.gymsaas.tenant.application.TenantCalendar;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.Map;
import java.util.UUID;

/**
 * Cancels a scheduled (future) price that never took effect and reopens the
 * price it would have superseded. Effective prices can never be cancelled.
 */
@Service
public class CancelPlanPriceUseCase {

    private final PlanGuard guard;
    private final PlanPriceRepository prices;
    private final TenantCalendar calendar;
    private final AuditService audit;

    public CancelPlanPriceUseCase(PlanGuard guard, PlanPriceRepository prices, TenantCalendar calendar,
                                  AuditService audit) {
        this.guard = guard;
        this.prices = prices;
        this.calendar = calendar;
        this.audit = audit;
    }

    @Transactional
    public PlanDtos.PriceView execute(UUID tenantId, UUID planId, UUID priceId, PlanDtos.CancelPriceRequest request) {
        guard.requirePlanForUpdate(tenantId, planId);
        PlanPrice price = prices.findById(tenantId, priceId)
                .filter(candidate -> candidate.planId().equals(planId))
                .orElseThrow(() -> ApiException.notFound("PRICE_NOT_FOUND", "Preço não encontrado."));
        LocalDate today = calendar.today(tenantId);
        if (price.status() != PlanPriceStatus.ACTIVE || !price.validFrom().isAfter(today)) {
            throw ApiException.conflict("PRICE_NOT_CANCELLABLE",
                    "Somente preços programados (vigência futura) podem ser cancelados.");
        }

        PlanPrice cancelled = prices.save(price.cancelled());
        LocalDate dayBefore = price.validFrom().minusDays(1);
        prices.findByPlan(tenantId, planId).stream()
                .filter(candidate -> candidate.status() == PlanPriceStatus.ACTIVE
                        && dayBefore.equals(candidate.validUntil()))
                .findFirst()
                .ifPresent(predecessor -> prices.save(predecessor.closedAt(price.validUntil())));

        audit.recordForTenant(tenantId, "PLAN_PRICE_CANCELLED", "plan", planId, "HIGH",
                Map.of("priceId", price.id().toString(), "status", "ACTIVE"),
                Map.of("priceId", price.id().toString(), "status", "CANCELLED"),
                Map.of("reason", request.reason().trim()));
        return PlanAssembler.price(cancelled, today);
    }
}
