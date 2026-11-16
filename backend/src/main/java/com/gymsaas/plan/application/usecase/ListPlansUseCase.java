package com.gymsaas.plan.application.usecase;

import com.gymsaas.plan.application.PlanAssembler;
import com.gymsaas.plan.application.PlanDtos;
import com.gymsaas.plan.application.PlanGuard;
import com.gymsaas.plan.domain.Plan;
import com.gymsaas.plan.domain.PlanPrice;
import com.gymsaas.plan.domain.PlanPriceRepository;
import com.gymsaas.plan.domain.PlanRepository;
import com.gymsaas.plan.domain.PlanSearchCriteria;
import com.gymsaas.plan.domain.PlanStatus;
import com.gymsaas.shared.domain.PageResult;
import com.gymsaas.tenant.application.TenantCalendar;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class ListPlansUseCase {

    private static final Set<String> ALLOWED_SORTS = Set.of("name", "code", "createdAt", "status");

    private final PlanGuard guard;
    private final PlanRepository plans;
    private final PlanPriceRepository prices;
    private final TenantCalendar calendar;

    public ListPlansUseCase(PlanGuard guard, PlanRepository plans, PlanPriceRepository prices,
                            TenantCalendar calendar) {
        this.guard = guard;
        this.plans = plans;
        this.prices = prices;
        this.calendar = calendar;
    }

    @Transactional(readOnly = true)
    public PageResult<PlanDtos.PlanSummary> execute(UUID tenantId, PlanDtos.PlanFilter filter) {
        int page = Math.max(0, filter.page());
        int size = filter.size() <= 0 ? 20 : Math.min(filter.size(), 100);
        String sort = filter.sort() != null && ALLOWED_SORTS.contains(filter.sort().trim())
                ? filter.sort().trim() : "name";
        String direction = "desc".equalsIgnoreCase(filter.direction()) ? "desc" : "asc";
        PlanStatus status = filter.status() == null || filter.status().isBlank()
                ? null : guard.status(filter.status());

        PageResult<Plan> result = plans.search(tenantId, new PlanSearchCriteria(
                PlanGuard.trimToNull(filter.search()), status, page, size, sort, direction));

        LocalDate today = calendar.today(tenantId);
        List<UUID> planIds = result.content().stream().map(Plan::id).toList();
        Map<UUID, PlanPrice> currentPrices = prices.findEffectiveForPlans(tenantId, planIds, today).stream()
                .collect(Collectors.toMap(PlanPrice::planId, price -> price, (first, second) -> first));

        List<PlanDtos.PlanSummary> content = result.content().stream()
                .map(plan -> PlanAssembler.summary(plan, currentPrices.get(plan.id()), today))
                .toList();
        return PageResult.of(content, result.totalElements(), page, size);
    }
}
