package com.gymsaas.shared.application;

import java.util.Set;
import java.util.UUID;

public record CurrentPrincipal(
        UUID userId,
        String email,
        UUID tenantId,
        UUID familyId,
        Set<String> roles,
        Set<String> permissions,
        boolean platformAdmin) {
}
