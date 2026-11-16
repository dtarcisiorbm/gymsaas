package com.gymsaas.student.infrastructure.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "student_notes")
public class StudentNoteJpaEntity {

    @Id
    UUID id;

    @Column(name = "tenant_id", nullable = false)
    UUID tenantId;

    @Column(name = "student_id", nullable = false)
    UUID studentId;

    @Column(nullable = false)
    String content;

    @Column(name = "created_by")
    UUID createdBy;

    @Column(name = "created_at", nullable = false)
    Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    Instant updatedAt;

    @Column(name = "deleted_at")
    Instant deletedAt;

    protected StudentNoteJpaEntity() {
    }

    StudentNoteJpaEntity(UUID id, UUID tenantId, UUID studentId, String content, UUID createdBy,
                         Instant createdAt, Instant updatedAt, Instant deletedAt) {
        this.id = id;
        this.tenantId = tenantId;
        this.studentId = studentId;
        this.content = content;
        this.createdBy = createdBy;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
        this.deletedAt = deletedAt;
    }
}
