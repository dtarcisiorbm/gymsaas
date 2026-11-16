package com.gymsaas.student.infrastructure.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "student_documents")
public class StudentDocumentJpaEntity {

    @Id
    UUID id;

    @Column(name = "tenant_id", nullable = false)
    UUID tenantId;

    @Column(name = "student_id", nullable = false)
    UUID studentId;

    @Column(name = "document_type", nullable = false)
    String documentType;

    @Column(name = "document_number", nullable = false)
    String documentNumber;

    @Column(name = "normalized_number", nullable = false)
    String normalizedNumber;

    String issuer;

    String state;

    @Column(name = "issued_at")
    LocalDate issuedAt;

    @Column(name = "expires_at")
    LocalDate expiresAt;

    String notes;

    @Column(name = "is_primary", nullable = false)
    boolean isPrimary;

    @Column(name = "created_at", nullable = false)
    Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    Instant updatedAt;

    @Column(name = "deleted_at")
    Instant deletedAt;

    protected StudentDocumentJpaEntity() {
    }

    StudentDocumentJpaEntity(UUID id, UUID tenantId, UUID studentId, String documentType,
                             String documentNumber, String normalizedNumber, String issuer,
                             String state, LocalDate issuedAt, LocalDate expiresAt, String notes,
                             boolean isPrimary, Instant createdAt, Instant updatedAt,
                             Instant deletedAt) {
        this.id = id;
        this.tenantId = tenantId;
        this.studentId = studentId;
        this.documentType = documentType;
        this.documentNumber = documentNumber;
        this.normalizedNumber = normalizedNumber;
        this.issuer = issuer;
        this.state = state;
        this.issuedAt = issuedAt;
        this.expiresAt = expiresAt;
        this.notes = notes;
        this.isPrimary = isPrimary;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
        this.deletedAt = deletedAt;
    }
}
