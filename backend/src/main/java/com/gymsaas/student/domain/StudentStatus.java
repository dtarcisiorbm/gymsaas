package com.gymsaas.student.domain;

import java.util.EnumSet;
import java.util.Map;
import java.util.Set;

public enum StudentStatus {
    PENDING,
    ACTIVE,
    INACTIVE,
    BLOCKED,
    ARCHIVED;

    private static final Map<StudentStatus, Set<StudentStatus>> TRANSITIONS = Map.of(
            PENDING, EnumSet.of(ACTIVE, INACTIVE, BLOCKED, ARCHIVED),
            ACTIVE, EnumSet.of(INACTIVE, BLOCKED, ARCHIVED),
            INACTIVE, EnumSet.of(ACTIVE, BLOCKED, ARCHIVED),
            BLOCKED, EnumSet.of(ACTIVE, INACTIVE, ARCHIVED),
            ARCHIVED, EnumSet.of(ACTIVE));

    public static StudentStatus parse(String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("Status is required.");
        }
        try {
            return StudentStatus.valueOf(value.trim().toUpperCase());
        } catch (IllegalArgumentException exception) {
            throw new IllegalArgumentException("Unknown student status: " + value);
        }
    }

    public boolean canTransitionTo(StudentStatus target) {
        return target != null && TRANSITIONS.getOrDefault(this, Set.of()).contains(target);
    }
}
