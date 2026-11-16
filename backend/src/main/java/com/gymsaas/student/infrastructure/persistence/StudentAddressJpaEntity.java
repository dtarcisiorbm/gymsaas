package com.gymsaas.student.infrastructure.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "student_addresses")
public class StudentAddressJpaEntity {

    @Id
    UUID id;

    @Column(name = "tenant_id", nullable = false)
    UUID tenantId;

    @Column(name = "student_id", nullable = false)
    UUID studentId;

    @Column(name = "zip_code")
    String zipCode;

    String street;

    @Column(name = "number")
    String addressNumber;

    String complement;

    String neighborhood;

    String city;

    String state;

    @Column(name = "created_at", nullable = false)
    Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    Instant updatedAt;

    protected StudentAddressJpaEntity() {
    }

    StudentAddressJpaEntity(UUID id, UUID tenantId, UUID studentId, String zipCode, String street,
                            String addressNumber, String complement, String neighborhood,
                            String city, String state, Instant createdAt, Instant updatedAt) {
        this.id = id;
        this.tenantId = tenantId;
        this.studentId = studentId;
        this.zipCode = zipCode;
        this.street = street;
        this.addressNumber = addressNumber;
        this.complement = complement;
        this.neighborhood = neighborhood;
        this.city = city;
        this.state = state;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }
}
