package com.gymsaas.plan.application.usecase;

import com.gymsaas.audit.application.AuditService;
import com.gymsaas.branch.domain.Branch;
import com.gymsaas.branch.domain.BranchRepository;
import com.gymsaas.plan.application.PlanDetailLoader;
import com.gymsaas.plan.application.PlanDtos;
import com.gymsaas.plan.application.PlanGuard;
import com.gymsaas.plan.domain.Plan;
import com.gymsaas.plan.domain.PlanFeature;
import com.gymsaas.plan.domain.PlanFeatureRepository;
import com.gymsaas.plan.domain.PlanFeatureType;
import com.gymsaas.shared.application.ApiException;
import com.gymsaas.shared.domain.UuidV7;
import com.gymsaas.tenant.application.TenantCalendar;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Replaces the plan's typed benefits. Memberships already contracted keep the
 * benefits captured in their conditions snapshot.
 */
@Service
public class ReplacePlanFeaturesUseCase {

    private final PlanGuard guard;
    private final PlanFeatureRepository features;
    private final BranchRepository branches;
    private final PlanDetailLoader detailLoader;
    private final TenantCalendar calendar;
    private final AuditService audit;

    public ReplacePlanFeaturesUseCase(PlanGuard guard, PlanFeatureRepository features, BranchRepository branches,
                                      PlanDetailLoader detailLoader, TenantCalendar calendar, AuditService audit) {
        this.guard = guard;
        this.features = features;
        this.branches = branches;
        this.detailLoader = detailLoader;
        this.calendar = calendar;
        this.audit = audit;
    }

    @Transactional
    public PlanDtos.PlanDetail execute(UUID tenantId, UUID planId, PlanDtos.ReplaceFeaturesRequest request) {
        Plan plan = guard.requirePlanForUpdate(tenantId, planId);
        Instant now = calendar.now();

        List<PlanFeature> candidates = new ArrayList<>();
        Set<PlanFeatureType> singletons = EnumSet.noneOf(PlanFeatureType.class);
        Set<UUID> branchAccess = new HashSet<>();
        for (int index = 0; index < request.features().size(); index++) {
            PlanDtos.FeatureRequest item = request.features().get(index);
            PlanFeatureType type;
            try {
                type = PlanFeatureType.parse(item.type());
            } catch (IllegalArgumentException exception) {
                throw ApiException.badRequest("INVALID_FEATURE_TYPE", "Tipo de benefício inválido.");
            }
            PlanFeature feature = new PlanFeature(UuidV7.generate(), tenantId, planId, type, item.branchId(),
                    item.intValue(), item.startTime(), item.endTime(), item.daysOfWeek(),
                    PlanGuard.trimToNull(item.label()), PlanGuard.trimToNull(item.description()), index, now);
            String violation = type.shapeViolation(feature);
            if (violation != null) {
                throw ApiException.badRequest("INVALID_FEATURE", violation);
            }
            if (PlanFeatureType.SINGLETONS.contains(type) && !singletons.add(type)) {
                throw ApiException.badRequest("DUPLICATE_FEATURE", "Benefício " + type.name() + " repetido.");
            }
            if (type == PlanFeatureType.BRANCH_ACCESS && !branchAccess.add(item.branchId())) {
                throw ApiException.badRequest("DUPLICATE_FEATURE", "Unidade repetida nos benefícios.");
            }
            candidates.add(feature);
        }

        Set<UUID> branchIds = candidates.stream().map(PlanFeature::branchId).filter(Objects::nonNull)
                .collect(Collectors.toSet());
        if (!branchIds.isEmpty()) {
            Set<UUID> found = branches.findAllByIds(tenantId, branchIds).stream().map(Branch::id)
                    .collect(Collectors.toSet());
            if (!found.containsAll(branchIds)) {
                throw ApiException.notFound("BRANCH_NOT_FOUND", "Unidade não encontrada.");
            }
        }

        features.replace(tenantId, planId, candidates);
        audit.recordForTenant(tenantId, "PLAN_FEATURES_REPLACED", "plan", planId, "MEDIUM", null,
                Map.of("features", candidates.stream().map(feature -> feature.type().name()).toList()), Map.of());
        return detailLoader.load(tenantId, plan);
    }
}
