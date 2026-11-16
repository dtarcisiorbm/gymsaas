package com.gymsaas.plan.domain;

import java.time.Instant;
import java.util.UUID;

public record Plan(
        UUID id,
        UUID tenantId,
        String code,
        String name,
        String description,
        DurationType durationType,
        int durationValue,
        PlanStatus status,
        FreezePolicy freezePolicy,
        String contractTerms,
        int version,
        UUID createdBy,
        Instant createdAt,
        Instant updatedAt) {

    public boolean active() {
        return status == PlanStatus.ACTIVE;
    }
}
