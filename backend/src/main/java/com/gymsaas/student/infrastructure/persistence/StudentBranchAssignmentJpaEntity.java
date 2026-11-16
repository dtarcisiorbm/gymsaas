package com.gymsaas.student.infrastructure.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "student_branch_assignments")
public class StudentBranchAssignmentJpaEntity {

    @Id
    UUID id;

    @Column(name = "tenant_id", nullable = false)
    UUID tenantId;

    @Column(name = "student_id", nullable = false)
    UUID studentId;

    @Column(name = "branch_id", nullable = false)
    UUID branchId;

    @Column(name = "is_primary", nullable = false)
    boolean isPrimary;

    @Column(name = "started_at", nullable = false)
    Instant startedAt;

    @Column(name = "ended_at")
    Instant endedAt;

    @Column(name = "transfer_reason")
    String transferReason;

    @Column(name = "assigned_by")
    UUID assignedBy;

    @Column(name = "created_at", nullable = false)
    Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    Instant updatedAt;

    protected StudentBranchAssignmentJpaEntity() {
    }

    StudentBranchAssignmentJpaEntity(UUID id, UUID tenantId, UUID studentId, UUID branchId,
                                     boolean isPrimary, Instant startedAt, Instant endedAt,
                                     String transferReason, UUID assignedBy, Instant createdAt,
                                     Instant updatedAt) {
        this.id = id;
        this.tenantId = tenantId;
        this.studentId = studentId;
        this.branchId = branchId;
        this.isPrimary = isPrimary;
        this.startedAt = startedAt;
        this.endedAt = endedAt;
        this.transferReason = transferReason;
        this.assignedBy = assignedBy;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }
}
