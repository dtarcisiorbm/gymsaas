package com.gymsaas.plan.infrastructure.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "plans")
public class PlanJpaEntity {

    @Id
    UUID id;

    @Column(name = "tenant_id", nullable = false)
    UUID tenantId;

    @Column(nullable = false)
    String code;

    @Column(nullable = false)
    String name;

    String description;

    @Column(name = "duration_type", nullable = false)
    String durationType;

    @Column(name = "duration_value", nullable = false)
    int durationValue;

    @Column(nullable = false)
    String status;

    @Column(name = "max_freeze_days")
    Integer maxFreezeDays;

    @Column(name = "max_freezes")
    Integer maxFreezes;

    @Column(name = "min_freeze_days")
    Integer minFreezeDays;

    @Column(name = "contract_terms")
    String contractTerms;

    @Version
    @Column(nullable = false)
    int version;

    @Column(name = "created_by")
    UUID createdBy;

    @Column(name = "created_at", nullable = false)
    Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    Instant updatedAt;

    protected PlanJpaEntity() {
    }

    PlanJpaEntity(UUID id, UUID tenantId, String code, UUID createdBy, Instant createdAt) {
        this.id = id;
        this.tenantId = tenantId;
        this.code = code;
        this.createdBy = createdBy;
        this.createdAt = createdAt;
    }
}
