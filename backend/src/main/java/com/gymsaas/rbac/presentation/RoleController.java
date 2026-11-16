package com.gymsaas.rbac.presentation;

import com.gymsaas.rbac.application.RbacService;
import com.gymsaas.rbac.domain.Role;
import com.gymsaas.shared.infrastructure.tenant.TenantContext;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Set;
import java.util.UUID;

@RestController
@RequestMapping("/api/roles")
public class RoleController {

    private final RbacService rbacService;

    public RoleController(RbacService rbacService) {
        this.rbacService = rbacService;
    }

    @GetMapping
    @PreAuthorize("hasAuthority('role:read')")
    public List<RoleResponse> list() {
        UUID tenantId = TenantContext.requireTenantId();
        return rbacService.listRoles(tenantId).stream().map(RoleResponse::from).toList();
    }

    @GetMapping("/{roleId}")
    @PreAuthorize("hasAuthority('role:read')")
    public RoleResponse detail(@PathVariable UUID roleId) {
        UUID tenantId = TenantContext.requireTenantId();
        return RoleResponse.from(rbacService.role(tenantId, roleId));
    }

    public record RoleResponse(UUID id, String code, String name, String description,
                               boolean system, Set<String> permissions) {

        static RoleResponse from(RbacService.RoleView view) {
            Role role = view.role();
            return new RoleResponse(role.id(), role.code(), role.name(), role.description(),
                    role.system(), view.permissions());
        }
    }
}
