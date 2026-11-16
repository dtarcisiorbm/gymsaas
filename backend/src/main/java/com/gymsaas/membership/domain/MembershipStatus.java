package com.gymsaas.membership.domain;

import java.util.EnumSet;
import java.util.Map;
import java.util.Set;

/**
 * PENDING   contracted, start date in the future
 * ACTIVE    running
 * SUSPENDED administratively suspended (does not extend the end date)
 * FROZEN    frozen on request (extends the end date by the frozen days)
 * EXPIRED   end date passed (terminal)
 * CANCELLED cancelled with reason (terminal)
 */
public enum MembershipStatus {
    PENDING,
    ACTIVE,
    SUSPENDED,
    FROZEN,
    EXPIRED,
    CANCELLED;

    public static final Set<MembershipStatus> OPEN = EnumSet.of(PENDING, ACTIVE, SUSPENDED, FROZEN);

    private static final Map<MembershipStatus, Set<MembershipStatus>> TRANSITIONS = Map.of(
            PENDING, EnumSet.of(ACTIVE, CANCELLED),
            ACTIVE, EnumSet.of(FROZEN, SUSPENDED, CANCELLED, EXPIRED),
            FROZEN, EnumSet.of(ACTIVE, CANCELLED),
            SUSPENDED, EnumSet.of(ACTIVE, CANCELLED, EXPIRED),
            EXPIRED, EnumSet.noneOf(MembershipStatus.class),
            CANCELLED, EnumSet.noneOf(MembershipStatus.class));

    public static MembershipStatus parse(String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("Status is required.");
        }
        try {
            return MembershipStatus.valueOf(value.trim().toUpperCase());
        } catch (IllegalArgumentException exception) {
            throw new IllegalArgumentException("Unknown membership status: " + value);
        }
    }

    public boolean open() {
        return OPEN.contains(this);
    }

    public boolean canTransitionTo(MembershipStatus target) {
        return target != null && TRANSITIONS.getOrDefault(this, Set.of()).contains(target);
    }
}
