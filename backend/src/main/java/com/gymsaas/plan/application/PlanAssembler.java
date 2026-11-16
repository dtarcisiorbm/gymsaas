package com.gymsaas.plan.application;

import com.gymsaas.branch.domain.Branch;
import com.gymsaas.plan.domain.FreezePolicy;
import com.gymsaas.plan.domain.Plan;
import com.gymsaas.plan.domain.PlanFeature;
import com.gymsaas.plan.domain.PlanPrice;
import com.gymsaas.plan.domain.PlanPriceStatus;

import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public final class PlanAssembler {

    private PlanAssembler() {
    }

    public static PlanDtos.PriceView price(PlanPrice price, LocalDate today) {
        if (price == null) {
            return null;
        }
        boolean scheduled = price.status() == PlanPriceStatus.ACTIVE && price.validFrom().isAfter(today);
        return new PlanDtos.PriceView(price.id(), price.amount(), price.currency(),
                price.billingPeriod().name(), price.billingInterval(), price.setupFee(), price.validFrom(),
                price.validUntil(), price.status().name(), price.notes(), price.effectiveOn(today), scheduled,
                price.createdAt());
    }

    public static PlanDtos.FreezePolicyView freezePolicy(FreezePolicy policy) {
        return policy == null ? new PlanDtos.FreezePolicyView(null, null, null)
                : new PlanDtos.FreezePolicyView(policy.maxFreezeDays(), policy.maxFreezes(), policy.minFreezeDays());
    }

    public static PlanDtos.FeatureView feature(PlanFeature feature, Map<UUID, Branch> branches) {
        Branch branch = feature.branchId() == null ? null : branches.get(feature.branchId());
        return new PlanDtos.FeatureView(feature.id(), feature.type().name(), feature.branchId(),
                branch == null ? null : branch.name(), feature.intValue(), feature.startTime(),
                feature.endTime(), feature.daysOfWeek(), feature.label(), feature.description(),
                feature.sortOrder());
    }

    public static PlanDtos.PlanSummary summary(Plan plan, PlanPrice currentPrice, LocalDate today) {
        return new PlanDtos.PlanSummary(plan.id(), plan.code(), plan.name(), plan.durationType().name(),
                plan.durationValue(), plan.status().name(), price(currentPrice, today), plan.updatedAt());
    }

    public static PlanDtos.PlanDetail detail(Plan plan, PlanPrice currentPrice, List<PlanFeature> features,
                                             Map<UUID, Branch> branches, LocalDate today) {
        List<PlanDtos.FeatureView> featureViews = features.stream()
                .sorted(Comparator.comparingInt(PlanFeature::sortOrder))
                .map(feature -> feature(feature, branches))
                .toList();
        return new PlanDtos.PlanDetail(plan.id(), plan.code(), plan.name(), plan.description(),
                plan.durationType().name(), plan.durationValue(), plan.status().name(),
                freezePolicy(plan.freezePolicy()), plan.contractTerms(), plan.version(),
                price(currentPrice, today), featureViews, plan.createdAt(), plan.updatedAt());
    }
}
