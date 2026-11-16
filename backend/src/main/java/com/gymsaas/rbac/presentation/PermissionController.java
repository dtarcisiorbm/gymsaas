package com.gymsaas.rbac.presentation;

import com.gymsaas.rbac.application.RbacService;
import com.gymsaas.rbac.domain.Permission;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/permissions")
public class PermissionController {

    private final RbacService rbacService;

    public PermissionController(RbacService rbacService) {
        this.rbacService = rbacService;
    }

    @GetMapping
    @PreAuthorize("hasAuthority('permission:read')")
    public List<PermissionResponse> list() {
        return rbacService.listPermissions().stream().map(PermissionResponse::from).toList();
    }

    public record PermissionResponse(UUID id, String code, String resource, String action,
                                     String description) {

        static PermissionResponse from(Permission permission) {
            return new PermissionResponse(permission.id(), permission.code(), permission.resource(),
                    permission.action(), permission.description());
        }
    }
}
