package com.gymsaas.student.domain;

import java.time.Instant;
import java.util.UUID;

public record StudentNote(
        UUID id,
        UUID tenantId,
        UUID studentId,
        String content,
        UUID createdBy,
        Instant createdAt,
        Instant updatedAt,
        Instant deletedAt) {
}
