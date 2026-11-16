package com.gymsaas.audit.infrastructure.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

@Entity
@Table(name = "audit_logs")
public class AuditLogJpaEntity {

    @Id
    private UUID id;

    @Column(name = "tenant_id")
    private UUID tenantId;

    @Column(name = "actor_type", nullable = false)
    private String actorType;

    @Column(name = "actor_id")
    private UUID actorId;

    @Column(nullable = false)
    private String action;

    @Column(name = "resource_type", nullable = false)
    private String resourceType;

    @Column(name = "resource_id")
    private UUID resourceId;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "old_values", columnDefinition = "jsonb")
    private Map<String, Object> oldValues;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "new_values", columnDefinition = "jsonb")
    private Map<String, Object> newValues;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb", nullable = false)
    private Map<String, Object> metadata;

    @Column(name = "risk_level", nullable = false)
    private String riskLevel;

    @Column(name = "occurred_at", nullable = false)
    private Instant occurredAt;

    protected AuditLogJpaEntity() {
    }

    public AuditLogJpaEntity(UUID id, UUID tenantId, String actorType, UUID actorId, String action,
                             String resourceType, UUID resourceId, Map<String, Object> oldValues,
                             Map<String, Object> newValues, Map<String, Object> metadata,
                             String riskLevel, Instant occurredAt) {
        this.id = id;
        this.tenantId = tenantId;
        this.actorType = actorType;
        this.actorId = actorId;
        this.action = action;
        this.resourceType = resourceType;
        this.resourceId = resourceId;
        this.oldValues = oldValues;
        this.newValues = newValues;
        this.metadata = metadata;
        this.riskLevel = riskLevel;
        this.occurredAt = occurredAt;
    }

    public UUID getId() {
        return id;
    }

    public UUID getTenantId() {
        return tenantId;
    }

    public String getActorType() {
        return actorType;
    }

    public UUID getActorId() {
        return actorId;
    }

    public String getAction() {
        return action;
    }

    public String getResourceType() {
        return resourceType;
    }

    public UUID getResourceId() {
        return resourceId;
    }

    public Map<String, Object> getOldValues() {
        return oldValues;
    }

    public Map<String, Object> getNewValues() {
        return newValues;
    }

    public Map<String, Object> getMetadata() {
        return metadata;
    }

    public String getRiskLevel() {
        return riskLevel;
    }

    public Instant getOccurredAt() {
        return occurredAt;
    }
}
