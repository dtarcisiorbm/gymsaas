package com.gymsaas.tenant.domain;

import java.util.Optional;

public enum TenantStatus {

    PENDING,
    ACTIVE,
    SUSPENDED,
    INACTIVE;

    public static Optional<TenantStatus> parse(String value) {
        if (value == null) {
            return Optional.empty();
        }
        try {
            return Optional.of(valueOf(value.trim().toUpperCase()));
        } catch (IllegalArgumentException exception) {
            return Optional.empty();
        }
    }
}
