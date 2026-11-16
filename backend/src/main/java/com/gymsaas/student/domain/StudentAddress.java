package com.gymsaas.student.domain;

import java.time.Instant;
import java.util.UUID;

public record StudentAddress(
        UUID id,
        UUID tenantId,
        UUID studentId,
        String zipCode,
        String street,
        String number,
        String complement,
        String neighborhood,
        String city,
        String state,
        Instant createdAt,
        Instant updatedAt) {
}
