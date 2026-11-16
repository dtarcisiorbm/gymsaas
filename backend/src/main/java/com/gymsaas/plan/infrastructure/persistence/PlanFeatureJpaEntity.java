package com.gymsaas.plan.infrastructure.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.time.LocalTime;
import java.util.UUID;

@Entity
@Table(name = "plan_features")
public class PlanFeatureJpaEntity {

    @Id
    UUID id;

    @Column(name = "tenant_id", nullable = false)
    UUID tenantId;

    @Column(name = "plan_id", nullable = false)
    UUID planId;

    @Column(name = "feature_type", nullable = false)
    String featureType;

    @Column(name = "branch_id")
    UUID branchId;

    @Column(name = "int_value")
    Integer intValue;

    @Column(name = "start_time")
    LocalTime startTime;

    @Column(name = "end_time")
    LocalTime endTime;

    @Column(name = "days_of_week")
    Short daysOfWeek;

    String label;

    String description;

    @Column(name = "sort_order", nullable = false)
    int sortOrder;

    @Column(name = "created_at", nullable = false)
    Instant createdAt;

    protected PlanFeatureJpaEntity() {
    }
}
