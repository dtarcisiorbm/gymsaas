package com.gymsaas.student.domain;

import java.time.Instant;
import java.util.UUID;

public record StudentBranchAssignment(
        UUID id,
        UUID tenantId,
        UUID studentId,
        UUID branchId,
        boolean primary,
        Instant startedAt,
        Instant endedAt,
        String transferReason,
        UUID assignedBy,
        Instant createdAt,
        Instant updatedAt) {
}
