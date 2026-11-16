package com.gymsaas.identity.infrastructure.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "users")
public class UserJpaEntity {

    @Id
    private UUID id;

    private String email;

    @Column(name = "password_hash")
    private String passwordHash;

    @Column(name = "full_name")
    private String fullName;

    private String phone;

    private String status;

    @Column(name = "mfa_enabled")
    private boolean mfaEnabled;

    @Column(name = "is_platform_admin")
    private boolean platformAdmin;

    @Column(name = "last_login_at")
    private Instant lastLoginAt;

    @Column(name = "created_at")
    private Instant createdAt;

    protected UserJpaEntity() {
    }

    public UserJpaEntity(UUID id, String email, String passwordHash, String fullName, String phone,
                         String status, boolean mfaEnabled, boolean platformAdmin,
                         Instant lastLoginAt, Instant createdAt) {
        this.id = id;
        this.email = email;
        this.passwordHash = passwordHash;
        this.fullName = fullName;
        this.phone = phone;
        this.status = status;
        this.mfaEnabled = mfaEnabled;
        this.platformAdmin = platformAdmin;
        this.lastLoginAt = lastLoginAt;
        this.createdAt = createdAt;
    }

    public UUID getId() {
        return id;
    }

    public String getEmail() {
        return email;
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public String getFullName() {
        return fullName;
    }

    public String getPhone() {
        return phone;
    }

    public String getStatus() {
        return status;
    }

    public boolean isMfaEnabled() {
        return mfaEnabled;
    }

    public boolean isPlatformAdmin() {
        return platformAdmin;
    }

    public Instant getLastLoginAt() {
        return lastLoginAt;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setLastLoginAt(Instant lastLoginAt) {
        this.lastLoginAt = lastLoginAt;
    }

    public void setFullName(String fullName) {
        this.fullName = fullName;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public void setStatus(String status) {
        this.status = status;
    }
}
