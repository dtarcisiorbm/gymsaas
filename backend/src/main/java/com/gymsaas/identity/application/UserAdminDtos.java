package com.gymsaas.identity.application;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.util.List;
import java.util.Set;
import java.util.UUID;

public final class UserAdminDtos {

    private UserAdminDtos() {
    }

    public record UserSummary(
            UUID id,
            String email,
            String fullName,
            String phone,
            String status,
            UUID membershipId,
            boolean defaultMembership,
            Set<String> roles,
            List<UUID> branchIds) {
    }

    public record UserBranchView(
            UUID id,
            String name,
            String code,
            String status) {
    }

    public record CreateUserRequest(
            @Email @NotBlank @Size(max = 150) String email,
            @NotBlank @Size(max = 200) String fullName,
            @NotBlank @Size(min = 10, max = 100) String password,
            @Size(max = 20) String phone,
            Set<UUID> roleIds,
            Set<UUID> branchIds) {
    }

    public record UpdateUserRequest(
            @NotBlank @Size(max = 200) String fullName,
            @Size(max = 20) String phone) {
    }

    public record AssignRolesRequest(@NotNull Set<UUID> roleIds) {
    }

    public record AssignBranchesRequest(@NotNull Set<UUID> branchIds) {
    }

    public record UpdateStatusRequest(
            @NotBlank @Pattern(regexp = "ACTIVE|INACTIVE|LOCKED", message = "Status inválido.")
            String status) {
    }
}
