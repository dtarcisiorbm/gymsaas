package com.gymsaas.contract.infrastructure.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.hibernate.annotations.Immutable;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.UUID;

/** Append-only evidence (trigger in V12). */
@Entity
@Immutable
@Table(name = "contract_acceptances")
public class ContractAcceptanceJpaEntity {

    @Id
    UUID id;

    @Column(name = "tenant_id", nullable = false)
    UUID tenantId;

    @Column(name = "contract_id", nullable = false)
    UUID contractId;

    @Column(name = "membership_id", nullable = false)
    UUID membershipId;

    @Column(name = "student_id", nullable = false)
    UUID studentId;

    @Column(name = "accepted_at", nullable = false)
    Instant acceptedAt;

    @Column(nullable = false)
    String channel;

    @Column(name = "acceptor_name", nullable = false)
    String acceptorName;

    @Column(name = "acceptor_document_masked")
    String acceptorDocumentMasked;

    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(name = "content_sha256", nullable = false, length = 64)
    String contentSha256;

    @Column(nullable = false)
    String declaration;

    @Column(name = "ip_address")
    String ipAddress;

    @Column(name = "user_agent")
    String userAgent;

    @Column(name = "registered_by")
    UUID registeredBy;

    @Column(name = "created_at", nullable = false)
    Instant createdAt;

    protected ContractAcceptanceJpaEntity() {
    }
}
