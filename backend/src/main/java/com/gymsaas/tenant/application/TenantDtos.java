package com.gymsaas.tenant.application;

import com.gymsaas.tenant.domain.Tenant;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.time.Instant;
import java.util.UUID;

public final class TenantDtos {

    private TenantDtos() {
    }

    public record TenantSummary(
            UUID id,
            String name,
            String document,
            String email,
            String status,
            Instant createdAt) {

        public static TenantSummary from(Tenant tenant) {
            return new TenantSummary(tenant.id(), tenant.name(), tenant.document(), tenant.email(),
                    tenant.status(), tenant.createdAt());
        }
    }

    public record TenantDetail(
            UUID id,
            String name,
            String legalName,
            String document,
            String email,
            String phone,
            String status,
            Instant createdAt,
            Instant updatedAt) {

        public static TenantDetail from(Tenant tenant) {
            return new TenantDetail(tenant.id(), tenant.name(), tenant.legalName(), tenant.document(),
                    tenant.email(), tenant.phone(), tenant.status(), tenant.createdAt(),
                    tenant.updatedAt());
        }
    }

    public record CreateTenantRequest(
            @NotBlank @Size(max = 150) String name,
            @NotBlank @Size(max = 200) String legalName,
            @NotBlank @Size(min = 11, max = 20) String document,
            @NotBlank @Email @Size(max = 150) String email,
            @Size(max = 20) String phone) {
    }

    public record UpdateTenantRequest(
            @NotBlank @Size(max = 150) String name,
            @NotBlank @Size(max = 200) String legalName,
            @NotBlank @Size(min = 11, max = 20) String document,
            @NotBlank @Email @Size(max = 150) String email,
            @Size(max = 20) String phone) {
    }

    public record ChangeTenantStatusRequest(
            @NotBlank
            @Pattern(regexp = "PENDING|ACTIVE|SUSPENDED|INACTIVE", message = "Status inválido.")
            String status) {
    }
}
