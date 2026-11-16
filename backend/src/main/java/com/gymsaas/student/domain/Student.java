package com.gymsaas.student.domain;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public record Student(
        UUID id,
        UUID tenantId,
        String registrationNumber,
        String name,
        String socialName,
        LocalDate birthDate,
        String email,
        String phone,
        String gender,
        String photoUrl,
        StudentStatus status,
        String notes,
        int version,
        Instant createdAt,
        Instant updatedAt,
        Instant deletedAt) {
}
