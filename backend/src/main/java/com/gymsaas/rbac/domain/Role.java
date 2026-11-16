package com.gymsaas.rbac.domain;

import java.util.UUID;

public record Role(
        UUID id,
        UUID tenantId,
        String code,
        String name,
        String description,
        boolean system) {
}
