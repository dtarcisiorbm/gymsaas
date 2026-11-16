package com.gymsaas.rbac.application;

import com.gymsaas.rbac.domain.Permission;
import com.gymsaas.rbac.domain.PermissionRepository;
import com.gymsaas.rbac.domain.Role;
import com.gymsaas.rbac.domain.RoleRepository;
import com.gymsaas.shared.application.ApiException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.UUID;

@Service
public class RbacService {

    private final RoleRepository roleRepository;
    private final PermissionRepository permissionRepository;

    public RbacService(RoleRepository roleRepository, PermissionRepository permissionRepository) {
        this.roleRepository = roleRepository;
        this.permissionRepository = permissionRepository;
    }

    @Transactional(readOnly = true)
    public List<RoleView> listRoles(UUID tenantId) {
        return roleRepository.findAssignableForTenant(tenantId).stream()
                .map(role -> new RoleView(role, permissionsOf(role.id())))
                .toList();
    }

    @Transactional(readOnly = true)
    public RoleView role(UUID tenantId, UUID roleId) {
        Role role = roleRepository.findById(roleId)
                .orElseThrow(() -> ApiException.notFound("ROLE_NOT_FOUND", "Papel não encontrado."));
        if (role.tenantId() != null && !role.tenantId().equals(tenantId)) {
            throw ApiException.notFound("ROLE_NOT_FOUND", "Papel não encontrado.");
        }
        return new RoleView(role, permissionsOf(role.id()));
    }

    @Transactional(readOnly = true)
    public List<Permission> listPermissions() {
        return permissionRepository.findAll();
    }

    private LinkedHashSet<String> permissionsOf(UUID roleId) {
        return new LinkedHashSet<>(roleRepository.findPermissionCodesByRoleIds(List.of(roleId)));
    }

    public record RoleView(Role role, LinkedHashSet<String> permissions) {
    }
}
