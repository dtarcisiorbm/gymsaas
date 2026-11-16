package com.gymsaas.branch.application;

import com.gymsaas.branch.domain.Branch;
import com.gymsaas.branch.domain.BranchSettings;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.time.Instant;
import java.time.LocalTime;
import java.util.Map;
import java.util.UUID;

public final class BranchDtos {

    private BranchDtos() {
    }

    public record BranchSummary(
            UUID id,
            String name,
            String code,
            String city,
            String state,
            String phone,
            String status,
            Instant createdAt) {

        public static BranchSummary from(Branch branch) {
            return new BranchSummary(branch.id(), branch.name(), branch.code(), branch.city(),
                    branch.state(), branch.phone(), branch.status(), branch.createdAt());
        }
    }

    public record BranchDetail(
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

        public static BranchDetail from(Branch branch) {
            return new BranchDetail(branch.id(), branch.tenantId(), branch.name(), branch.code(),
                    branch.document(), branch.email(), branch.phone(), branch.zipCode(),
                    branch.street(), branch.number(), branch.complement(), branch.neighborhood(),
                    branch.city(), branch.state(), branch.status(), branch.createdAt(),
                    branch.updatedAt());
        }
    }

    public record CreateBranchRequest(
            @NotBlank @Size(max = 150) String name,
            @NotBlank @Pattern(regexp = "[A-Za-z0-9_-]{2,30}", message = "Código inválido.") String code,
            @Pattern(regexp = "\\d{11,14}", message = "Documento inválido.") String document,
            @Email @Size(max = 150) String email,
            @Size(max = 20) String phone,
            @Pattern(regexp = "\\d{5}-?\\d{3}", message = "CEP inválido.") String zipCode,
            @Size(max = 200) String street,
            @Size(max = 20) String number,
            @Size(max = 100) String complement,
            @Size(max = 100) String neighborhood,
            @Size(max = 100) String city,
            @Pattern(regexp = "[A-Za-z]{2}", message = "UF inválida.") String state) {
    }

    public record UpdateBranchRequest(
            @NotBlank @Size(max = 150) String name,
            @NotBlank @Pattern(regexp = "[A-Za-z0-9_-]{2,30}", message = "Código inválido.") String code,
            @Pattern(regexp = "\\d{11,14}", message = "Documento inválido.") String document,
            @Email @Size(max = 150) String email,
            @Size(max = 20) String phone,
            @Pattern(regexp = "\\d{5}-?\\d{3}", message = "CEP inválido.") String zipCode,
            @Size(max = 200) String street,
            @Size(max = 20) String number,
            @Size(max = 100) String complement,
            @Size(max = 100) String neighborhood,
            @Size(max = 100) String city,
            @Pattern(regexp = "[A-Za-z]{2}", message = "UF inválida.") String state) {
    }

    public record ChangeBranchStatusRequest(
            @NotBlank
            @Pattern(regexp = "ACTIVE|INACTIVE", message = "Status inválido.")
            String status) {
    }

    public record BranchSettingsView(
            UUID id,
            UUID branchId,
            String timezone,
            LocalTime openingTime,
            LocalTime closingTime,
            Integer maxCapacity,
            boolean allow24hAccess,
            Map<String, Object> settings) {

        public static BranchSettingsView from(BranchSettings settings) {
            return new BranchSettingsView(settings.id(), settings.branchId(), settings.timezone(),
                    settings.openingTime(), settings.closingTime(), settings.maxCapacity(),
                    settings.allow24hAccess(), settings.settings());
        }
    }

    public record UpdateBranchSettingsRequest(
            @Size(max = 50) String timezone,
            LocalTime openingTime,
            LocalTime closingTime,
            @Min(value = 1, message = "Capacidade deve ser positiva.") Integer maxCapacity,
            Boolean allow24hAccess,
            Map<String, Object> settings) {
    }
}
