package com.gymsaas.plan.domain;

import java.time.Instant;
import java.time.LocalTime;
import java.util.UUID;

/**
 * Typed benefit. daysOfWeek is a bitmask (Monday = 1 ... Sunday = 64).
 */
public record PlanFeature(
        UUID id,
        UUID tenantId,
        UUID planId,
        PlanFeatureType type,
        UUID branchId,
        Integer intValue,
        LocalTime startTime,
        LocalTime endTime,
        Integer daysOfWeek,
        String label,
        String description,
        int sortOrder,
        Instant createdAt) {
}
