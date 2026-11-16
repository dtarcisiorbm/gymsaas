package com.gymsaas.student.domain;

import java.time.Instant;
import java.util.UUID;

public record StudentContact(
        UUID id,
        UUID tenantId,
        UUID studentId,
        String name,
        String relationship,
        String phone,
        String email,
        boolean primary,
        String notes,
        Instant createdAt,
        Instant updatedAt,
        Instant deletedAt) {
}
