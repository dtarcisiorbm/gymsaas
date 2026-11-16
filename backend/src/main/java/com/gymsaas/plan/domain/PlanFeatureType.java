package com.gymsaas.plan.domain;

import java.util.EnumSet;
import java.util.Set;

/**
 * Typed plan benefits. The shape rules mirror ck_plan_features_shape (V10).
 */
public enum PlanFeatureType {
    ALL_BRANCHES_ACCESS,
    BRANCH_ACCESS,
    TIME_WINDOW,
    DAILY_CHECKIN_LIMIT,
    WEEKLY_CHECKIN_LIMIT,
    GUEST_PASSES_PER_MONTH,
    CUSTOM;

    /** Types that may appear at most once per plan. */
    public static final Set<PlanFeatureType> SINGLETONS = EnumSet.of(
            ALL_BRANCHES_ACCESS, DAILY_CHECKIN_LIMIT, WEEKLY_CHECKIN_LIMIT, GUEST_PASSES_PER_MONTH);

    public static PlanFeatureType parse(String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("Feature type is required.");
        }
        try {
            return PlanFeatureType.valueOf(value.trim().toUpperCase());
        } catch (IllegalArgumentException exception) {
            throw new IllegalArgumentException("Unknown feature type: " + value);
        }
    }

    /** Returns null when the feature has the shape required by its type. */
    public String shapeViolation(PlanFeature feature) {
        return switch (this) {
            case ALL_BRANCHES_ACCESS -> feature.branchId() == null && feature.intValue() == null
                    && feature.startTime() == null && feature.endTime() == null
                    ? null : "ALL_BRANCHES_ACCESS não aceita unidade, valor ou horário.";
            case BRANCH_ACCESS -> feature.branchId() != null && feature.intValue() == null
                    ? null : "BRANCH_ACCESS exige uma unidade.";
            case TIME_WINDOW -> feature.startTime() != null && feature.endTime() != null
                    && feature.startTime().isBefore(feature.endTime())
                    && feature.daysOfWeek() != null && feature.daysOfWeek() >= 1 && feature.daysOfWeek() <= 127
                    ? null : "TIME_WINDOW exige início < fim e ao menos um dia da semana.";
            case DAILY_CHECKIN_LIMIT, WEEKLY_CHECKIN_LIMIT, GUEST_PASSES_PER_MONTH ->
                    feature.intValue() != null && feature.intValue() > 0
                            ? null : name() + " exige um valor inteiro positivo.";
            case CUSTOM -> feature.label() != null && !feature.label().isBlank()
                    ? null : "CUSTOM exige um rótulo.";
        };
    }
}
