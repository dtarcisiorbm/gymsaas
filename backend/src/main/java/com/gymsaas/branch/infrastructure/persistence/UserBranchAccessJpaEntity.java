package com.gymsaas.branch.infrastructure.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "user_branch_access")
public class UserBranchAccessJpaEntity {

    @Id
    private UUID id;

    @Column(name = "tenant_id", nullable = false)
    private UUID tenantId;

    @Column(name = "user_tenant_id", nullable = false)
    private UUID userTenantId;

    @Column(name = "branch_id", nullable = false)
    private UUID branchId;

    @Column(name = "granted_by")
    private UUID grantedBy;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    protected UserBranchAccessJpaEntity() {
    }

    public UserBranchAccessJpaEntity(UUID id, UUID tenantId, UUID userTenantId, UUID branchId,
                                     UUID grantedBy, Instant createdAt) {
        this.id = id;
        this.tenantId = tenantId;
        this.userTenantId = userTenantId;
        this.branchId = branchId;
        this.grantedBy = grantedBy;
        this.createdAt = createdAt;
    }

    public UUID getId() {
        return id;
    }

    public UUID getTenantId() {
        return tenantId;
    }

    public UUID getUserTenantId() {
        return userTenantId;
    }

    public UUID getBranchId() {
        return branchId;
    }

    public UUID getGrantedBy() {
        return grantedBy;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
