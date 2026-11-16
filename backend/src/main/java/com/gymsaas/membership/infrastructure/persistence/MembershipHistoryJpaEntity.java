package com.gymsaas.membership.infrastructure.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.hibernate.annotations.Immutable;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

/** Append-only (trigger in V11); mapped as immutable so Hibernate never issues UPDATEs. */
@Entity
@Immutable
@Table(name = "membership_status_history")
public class MembershipHistoryJpaEntity {

    @Id
    UUID id;

    @Column(name = "tenant_id", nullable = false)
    UUID tenantId;

    @Column(name = "membership_id", nullable = false)
    UUID membershipId;

    @Column(name = "event_type", nullable = false)
    String eventType;

    @Column(name = "from_status")
    String fromStatus;

    @Column(name = "to_status")
    String toStatus;

    String reason;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb", nullable = false)
    Map<String, Object> metadata;

    @Column(name = "changed_by")
    UUID changedBy;

    @Column(name = "changed_at", nullable = false)
    Instant changedAt;

    protected MembershipHistoryJpaEntity() {
    }
}
