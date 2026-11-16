package com.gymsaas.student.domain;

import java.time.Instant;
import java.util.UUID;

public record StudentStatusEntry(
        UUID id,
        UUID tenantId,
        UUID studentId,
        StudentStatus fromStatus,
        StudentStatus toStatus,
        String reason,
        UUID changedBy,
        Instant changedAt) {
}
