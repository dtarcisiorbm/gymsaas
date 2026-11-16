package com.gymsaas.plan.domain;

import java.util.EnumSet;
import java.util.Map;
import java.util.Set;

/** Only ACTIVE plans accept new memberships and renewals. */
public enum PlanStatus {
    ACTIVE,
    INACTIVE,
    ARCHIVED;

    private static final Map<PlanStatus, Set<PlanStatus>> TRANSITIONS = Map.of(
            ACTIVE, EnumSet.of(INACTIVE, ARCHIVED),
            INACTIVE, EnumSet.of(ACTIVE, ARCHIVED),
            ARCHIVED, EnumSet.of(INACTIVE));

    public static PlanStatus parse(String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("Status is required.");
        }
        try {
            return PlanStatus.valueOf(value.trim().toUpperCase());
        } catch (IllegalArgumentException exception) {
            throw new IllegalArgumentException("Unknown plan status: " + value);
        }
    }

    public boolean canTransitionTo(PlanStatus target) {
        return target != null && TRANSITIONS.getOrDefault(this, Set.of()).contains(target);
    }
}
