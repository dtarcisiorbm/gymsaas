package com.gymsaas.student.infrastructure.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "student_status_history")
public class StudentStatusHistoryJpaEntity {

    @Id
    UUID id;

    @Column(name = "tenant_id", nullable = false)
    UUID tenantId;

    @Column(name = "student_id", nullable = false)
    UUID studentId;

    @Column(name = "from_status")
    String fromStatus;

    @Column(name = "to_status", nullable = false)
    String toStatus;

    String reason;

    @Column(name = "changed_by")
    UUID changedBy;

    @Column(name = "changed_at", nullable = false)
    Instant changedAt;

    protected StudentStatusHistoryJpaEntity() {
    }

    StudentStatusHistoryJpaEntity(UUID id, UUID tenantId, UUID studentId, String fromStatus,
                                  String toStatus, String reason, UUID changedBy, Instant changedAt) {
        this.id = id;
        this.tenantId = tenantId;
        this.studentId = studentId;
        this.fromStatus = fromStatus;
        this.toStatus = toStatus;
        this.reason = reason;
        this.changedBy = changedBy;
        this.changedAt = changedAt;
    }
}
