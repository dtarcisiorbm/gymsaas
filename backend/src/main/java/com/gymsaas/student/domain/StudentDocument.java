package com.gymsaas.student.domain;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public record StudentDocument(
        UUID id,
        UUID tenantId,
        UUID studentId,
        String type,
        String number,
        String normalizedNumber,
        String issuer,
        String state,
        LocalDate issuedAt,
        LocalDate expiresAt,
        String notes,
        boolean primary,
        Instant createdAt,
        Instant updatedAt,
        Instant deletedAt) {
}
