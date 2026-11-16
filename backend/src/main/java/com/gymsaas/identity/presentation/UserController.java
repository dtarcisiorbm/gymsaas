package com.gymsaas.identity.presentation;

import com.gymsaas.identity.application.UserAdminDtos;
import com.gymsaas.identity.application.UserAdminService;
import com.gymsaas.shared.domain.PageResult;
import com.gymsaas.shared.infrastructure.tenant.TenantContext;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/users")
@Tag(name = "Usuários")
public class UserController {

    private final UserAdminService userAdminService;

    public UserController(UserAdminService userAdminService) {
        this.userAdminService = userAdminService;
    }

    @GetMapping
    @PreAuthorize("hasAuthority('users:read') or hasAuthority('user:read')")
    @Operation(summary = "Lista usuários vinculados à academia ativa")
    public PageResult<UserAdminDtos.UserSummary> list(
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String search,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return userAdminService.list(TenantContext.requireTenantId(), status, search, page, size);
    }

    @PostMapping
    @PreAuthorize("hasAuthority('users:create') or hasAuthority('user:create')")
    @Operation(summary = "Cria ou vincula um usuário à academia ativa")
    public UserAdminDtos.UserSummary create(
            @Valid @RequestBody UserAdminDtos.CreateUserRequest request) {
        return userAdminService.create(TenantContext.requireTenantId(), request);
    }

    @PutMapping("/{userId}")
    @PreAuthorize("hasAuthority('users:update') or hasAuthority('user:update')")
    public UserAdminDtos.UserSummary update(@PathVariable UUID userId,
                                            @Valid @RequestBody UserAdminDtos.UpdateUserRequest request) {
        return userAdminService.update(TenantContext.requireTenantId(), userId, request);
    }

    @PatchMapping("/{userId}/status")
    @PreAuthorize("hasAuthority('users:deactivate') or hasAuthority('user:update')")
    @Operation(summary = "Ativa, desativa ou bloqueia um usuário")
    public UserAdminDtos.UserSummary updateStatus(
            @PathVariable UUID userId,
            @Valid @RequestBody UserAdminDtos.UpdateStatusRequest request) {
        return userAdminService.updateStatus(TenantContext.requireTenantId(), userId, request);
    }

    @PutMapping("/{userId}/roles")
    @PreAuthorize("hasAuthority('users:manage_roles') or hasAuthority('user:assign_role')")
    public UserAdminDtos.UserSummary assignRoles(
            @PathVariable UUID userId,
            @Valid @RequestBody UserAdminDtos.AssignRolesRequest request) {
        return userAdminService.assignRoles(TenantContext.requireTenantId(), userId, request);
    }

    @GetMapping("/{userId}/branches")
    @PreAuthorize("hasAuthority('users:read') or hasAuthority('user:read')")
    public List<UserAdminDtos.UserBranchView> branches(@PathVariable UUID userId) {
        return userAdminService.branchAccess(TenantContext.requireTenantId(), userId);
    }

    @PutMapping("/{userId}/branches")
    @PreAuthorize("hasAuthority('users:assign_branch')")
    @Operation(summary = "Substitui as unidades vinculadas ao usuário")
    public List<UserAdminDtos.UserBranchView> replaceBranches(
            @PathVariable UUID userId,
            @Valid @RequestBody UserAdminDtos.AssignBranchesRequest request) {
        return userAdminService.replaceBranches(TenantContext.requireTenantId(), userId, request);
    }

    @PostMapping("/{userId}/branches/{branchId}")
    @PreAuthorize("hasAuthority('users:assign_branch')")
    public List<UserAdminDtos.UserBranchView> assignBranch(@PathVariable UUID userId,
                                                           @PathVariable UUID branchId) {
        return userAdminService.assignBranch(TenantContext.requireTenantId(), userId, branchId);
    }

    @DeleteMapping("/{userId}/branches/{branchId}")
    @PreAuthorize("hasAuthority('users:assign_branch')")
    public List<UserAdminDtos.UserBranchView> removeBranch(@PathVariable UUID userId,
                                                           @PathVariable UUID branchId) {
        return userAdminService.removeBranch(TenantContext.requireTenantId(), userId, branchId);
    }
}
