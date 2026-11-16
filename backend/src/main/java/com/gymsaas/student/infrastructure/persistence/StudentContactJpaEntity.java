package com.gymsaas.student.infrastructure.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "student_contacts")
public class StudentContactJpaEntity {

    @Id
    UUID id;

    @Column(name = "tenant_id", nullable = false)
    UUID tenantId;

    @Column(name = "student_id", nullable = false)
    UUID studentId;

    @Column(nullable = false)
    String name;

    String relationship;

    @Column(nullable = false)
    String phone;

    String email;

    @Column(name = "is_primary", nullable = false)
    boolean isPrimary;

    String notes;

    @Column(name = "created_at", nullable = false)
    Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    Instant updatedAt;

    @Column(name = "deleted_at")
    Instant deletedAt;

    protected StudentContactJpaEntity() {
    }

    StudentContactJpaEntity(UUID id, UUID tenantId, UUID studentId, String name,
                            String relationship, String phone, String email, boolean isPrimary,
                            String notes, Instant createdAt, Instant updatedAt, Instant deletedAt) {
        this.id = id;
        this.tenantId = tenantId;
        this.studentId = studentId;
        this.name = name;
        this.relationship = relationship;
        this.phone = phone;
        this.email = email;
        this.isPrimary = isPrimary;
        this.notes = notes;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
        this.deletedAt = deletedAt;
    }
}
