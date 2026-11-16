package com.gymsaas.tenant.infrastructure.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "tenants")
public class TenantJpaEntity {

    @Id
    private UUID id;

    private String name;

    @Column(name = "legal_name")
    private String legalName;

    private String document;

    private String cnpj;

    private String email;

    private String phone;

    private String status;

    @Column(name = "created_at")
    private Instant createdAt;

    @Column(name = "updated_at")
    private Instant updatedAt;

    protected TenantJpaEntity() {
    }

    public TenantJpaEntity(UUID id, String name, String legalName, String document, String email,
                           String phone, String status, Instant createdAt, Instant updatedAt) {
        this.id = id;
        this.name = name;
        this.legalName = legalName;
        this.document = document;
        this.cnpj = document;
        this.email = email;
        this.phone = phone;
        this.status = status;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public UUID getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getLegalName() {
        return legalName;
    }

    public String getDocument() {
        return document;
    }

    public String getCnpj() {
        return cnpj;
    }

    public String getEmail() {
        return email;
    }

    public String getPhone() {
        return phone;
    }

    public String getStatus() {
        return status;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setLegalName(String legalName) {
        this.legalName = legalName;
    }

    public void setDocument(String document) {
        this.document = document;
        this.cnpj = document;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public void setStatus(String status) {
        this.status = status;
    }
}
