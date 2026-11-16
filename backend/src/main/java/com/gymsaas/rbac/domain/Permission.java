package com.gymsaas.rbac.domain;

import java.util.UUID;

public record Permission(
        UUID id,
        String code,
        String resource,
        String action,
        String description) {
}
