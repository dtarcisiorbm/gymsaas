package com.gymsaas.membership.infrastructure.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "membership_freezes")
public class MembershipFreezeJpaEntity {

    @Id
    UUID id;

    @Column(name = "tenant_id", nullable = false)
    UUID tenantId;

    @Column(name = "membership_id", nullable = false)
    UUID membershipId;

    @Column(name = "start_date", nullable = false)
    LocalDate startDate;

    @Column(name = "planned_end_date", nullable = false)
    LocalDate plannedEndDate;

    @Column(name = "actual_end_date")
    LocalDate actualEndDate;

    @Column(name = "planned_days", nullable = false)
    int plannedDays;

    @Column(name = "effective_days")
    Integer effectiveDays;

    @Column(nullable = false)
    String status;

    @Column(nullable = false)
    String reason;

    @Column(name = "created_by")
    UUID createdBy;

    @Column(name = "ended_by")
    UUID endedBy;

    @Column(name = "created_at", nullable = false)
    Instant createdAt;

    @Column(name = "ended_at")
    Instant endedAt;

    protected MembershipFreezeJpaEntity() {
    }
}
