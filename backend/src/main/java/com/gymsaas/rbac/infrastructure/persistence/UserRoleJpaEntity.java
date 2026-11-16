package com.gymsaas.rbac.infrastructure.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "user_roles")
public class UserRoleJpaEntity {

    @Id
    private UUID id;

    @Column(name = "user_tenant_id", nullable = false)
    private UUID userTenantId;

    @Column(name = "tenant_id", nullable = false)
    private UUID tenantId;

    @Column(name = "role_id", nullable = false)
    private UUID roleId;

    @Column(name = "assigned_by")
    private UUID assignedBy;

    @Column(name = "assigned_at", nullable = false)
    private Instant assignedAt;

    protected UserRoleJpaEntity() {
    }

    public UserRoleJpaEntity(UUID id, UUID userTenantId, UUID tenantId, UUID roleId,
                             UUID assignedBy, Instant assignedAt) {
        this.id = id;
        this.userTenantId = userTenantId;
        this.tenantId = tenantId;
        this.roleId = roleId;
        this.assignedBy = assignedBy;
        this.assignedAt = assignedAt;
    }

    public UUID getId() {
        return id;
    }

    public UUID getUserTenantId() {
        return userTenantId;
    }

    public UUID getTenantId() {
        return tenantId;
    }

    public UUID getRoleId() {
        return roleId;
    }

    public UUID getAssignedBy() {
        return assignedBy;
    }

    public Instant getAssignedAt() {
        return assignedAt;
    }
}
