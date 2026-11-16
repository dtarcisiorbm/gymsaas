package com.gymsaas.plan.application.usecase;

import com.gymsaas.plan.application.PlanAssembler;
import com.gymsaas.plan.application.PlanDtos;
import com.gymsaas.plan.application.PlanGuard;
import com.gymsaas.plan.domain.PlanPriceRepository;
import com.gymsaas.tenant.application.TenantCalendar;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/** Full price history of a plan (newest validity first). */
@Service
public class ListPlanPricesUseCase {

    private final PlanGuard guard;
    private final PlanPriceRepository prices;
    private final TenantCalendar calendar;

    public ListPlanPricesUseCase(PlanGuard guard, PlanPriceRepository prices, TenantCalendar calendar) {
        this.guard = guard;
        this.prices = prices;
        this.calendar = calendar;
    }

    @Transactional(readOnly = true)
    public List<PlanDtos.PriceView> execute(UUID tenantId, UUID planId) {
        guard.requirePlan(tenantId, planId);
        LocalDate today = calendar.today(tenantId);
        return prices.findByPlan(tenantId, planId).stream()
                .map(price -> PlanAssembler.price(price, today))
                .toList();
    }
}
