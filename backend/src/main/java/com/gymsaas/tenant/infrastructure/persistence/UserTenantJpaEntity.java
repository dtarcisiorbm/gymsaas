package com.gymsaas.tenant.infrastructure.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.util.UUID;

@Entity
@Table(name = "user_tenants")
public class UserTenantJpaEntity {

    @Id
    private UUID id;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "tenant_id", nullable = false)
    private UUID tenantId;

    @Column(name = "is_default", nullable = false)
    private boolean defaultMembership;

    @Column(nullable = false)
    private String status;

    protected UserTenantJpaEntity() {
    }

    public UserTenantJpaEntity(UUID id, UUID userId, UUID tenantId, boolean defaultMembership, String status) {
        this.id = id;
        this.userId = userId;
        this.tenantId = tenantId;
        this.defaultMembership = defaultMembership;
        this.status = status;
    }

    public UUID getId() {
        return id;
    }

    public UUID getUserId() {
        return userId;
    }

    public UUID getTenantId() {
        return tenantId;
    }

    public boolean isDefaultMembership() {
        return defaultMembership;
    }

    public String getStatus() {
        return status;
    }
}
