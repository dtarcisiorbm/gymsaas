package com.gymsaas.branch.domain;

import java.time.Instant;
import java.util.UUID;

public record Branch(
        UUID id,
        UUID tenantId,
        String name,
        String code,
        String document,
        String email,
        String phone,
        String zipCode,
        String street,
        String number,
        String complement,
        String neighborhood,
        String city,
        String state,
        String status,
        Instant createdAt,
        Instant updatedAt) {

    public boolean active() {
        return "ACTIVE".equals(status);
    }
}
