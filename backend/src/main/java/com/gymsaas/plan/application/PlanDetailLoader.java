package com.gymsaas.plan.application;

import com.gymsaas.branch.domain.Branch;
import com.gymsaas.branch.domain.BranchRepository;
import com.gymsaas.plan.domain.Plan;
import com.gymsaas.plan.domain.PlanFeature;
import com.gymsaas.plan.domain.PlanFeatureRepository;
import com.gymsaas.plan.domain.PlanPriceRepository;
import com.gymsaas.tenant.application.TenantCalendar;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Collectors;

/** Builds the plan detail view (current price + features with branch names). */
@Component
public class PlanDetailLoader {

    private final PlanPriceRepository prices;
    private final PlanFeatureRepository features;
    private final BranchRepository branches;
    private final TenantCalendar calendar;

    public PlanDetailLoader(PlanPriceRepository prices, PlanFeatureRepository features,
                            BranchRepository branches, TenantCalendar calendar) {
        this.prices = prices;
        this.features = features;
        this.branches = branches;
        this.calendar = calendar;
    }

    public PlanDtos.PlanDetail load(UUID tenantId, Plan plan) {
        LocalDate today = calendar.today(tenantId);
        List<PlanFeature> planFeatures = features.findByPlan(tenantId, plan.id());
        List<UUID> branchIds = planFeatures.stream().map(PlanFeature::branchId)
                .filter(Objects::nonNull).distinct().toList();
        Map<UUID, Branch> branchMap = branches.findAllByIds(tenantId, branchIds).stream()
                .collect(Collectors.toMap(Branch::id, branch -> branch));
        return PlanAssembler.detail(plan, prices.findEffective(tenantId, plan.id(), today).orElse(null),
                planFeatures, branchMap, today);
    }
}
