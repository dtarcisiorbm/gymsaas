package com.gymsaas.student.infrastructure.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "students")
public class StudentJpaEntity {

    @Id
    UUID id;

    @Column(name = "tenant_id", nullable = false)
    UUID tenantId;

    @Column(name = "registration_number", nullable = false)
    String registrationNumber;

    @Column(nullable = false)
    String name;

    @Column(name = "social_name")
    String socialName;

    @Column(name = "birth_date")
    LocalDate birthDate;

    String email;

    String phone;

    String gender;

    @Column(name = "photo_url")
    String photoUrl;

    @Column(nullable = false)
    String status;

    String notes;

    @Version
    @Column(nullable = false)
    int version;

    @Column(name = "created_at", nullable = false)
    Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    Instant updatedAt;

    @Column(name = "deleted_at")
    Instant deletedAt;

    protected StudentJpaEntity() {
    }

    StudentJpaEntity(UUID id, UUID tenantId, String registrationNumber, String name,
                     String socialName, LocalDate birthDate, String email, String phone,
                     String gender, String photoUrl, String status, String notes, int version,
                     Instant createdAt, Instant updatedAt, Instant deletedAt) {
        this.id = id;
        this.tenantId = tenantId;
        this.registrationNumber = registrationNumber;
        this.name = name;
        this.socialName = socialName;
        this.birthDate = birthDate;
        this.email = email;
        this.phone = phone;
        this.gender = gender;
        this.photoUrl = photoUrl;
        this.status = status;
        this.notes = notes;
        this.version = version;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
        this.deletedAt = deletedAt;
    }
}
