package com.gymsaas.identity.application;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.List;
import java.util.Set;
import java.util.UUID;

public final class AuthDtos {

    private AuthDtos() {
    }

    public record LoginRequest(
            @Email @NotBlank String email,
            @NotBlank String password,
            UUID tenantId) {
    }

    public record SelectTenantRequest(@NotNull UUID tenantId) {
    }

    public record UserView(UUID id, String email, String fullName, String status) {
    }

    public record TenantView(UUID id, String name, boolean defaultMembership, Set<String> roles) {
    }

    public record SessionResponse(
            String accessToken,
            String tokenType,
            long expiresIn,
            UserView user,
            List<TenantView> tenants,
            UUID activeTenantId,
            boolean platformAdmin) {
    }

    public record TenantSelectionResponse(
            String accessToken,
            String tokenType,
            long expiresIn,
            TenantView tenant) {
    }

    public record MeResponse(
            UserView user,
            TenantView tenant,
            Set<String> roles,
            Set<String> permissions,
            boolean platformAdmin) {
    }
}
