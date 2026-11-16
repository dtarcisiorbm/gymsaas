package com.gymsaas.contract.infrastructure.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "membership_contracts")
public class MembershipContractJpaEntity {

    @Id
    UUID id;

    @Column(name = "tenant_id", nullable = false)
    UUID tenantId;

    @Column(name = "membership_id", nullable = false)
    UUID membershipId;

    @Column(name = "contract_version", nullable = false)
    int contractVersion;

    @Column(name = "template_version", nullable = false)
    String templateVersion;

    @Column(nullable = false)
    String status;

    @Column(nullable = false)
    String content;

    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(name = "content_sha256", nullable = false, length = 64)
    String contentSha256;

    @Column(name = "storage_key", nullable = false)
    String storageKey;

    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(name = "document_sha256", nullable = false, length = 64)
    String documentSha256;

    @Column(name = "document_size_bytes", nullable = false)
    long documentSizeBytes;

    @Column(name = "document_content_type", nullable = false)
    String documentContentType;

    @Column(name = "generated_by")
    UUID generatedBy;

    @Column(name = "generated_at", nullable = false)
    Instant generatedAt;

    @Column(name = "status_changed_at")
    Instant statusChangedAt;

    protected MembershipContractJpaEntity() {
    }
}
