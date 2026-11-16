package com.gymsaas.membership.infrastructure.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Map;
import java.util.UUID;

@Entity
@Table(name = "student_memberships")
public class StudentMembershipJpaEntity {

    @Id
    UUID id;

    @Column(name = "tenant_id", nullable = false)
    UUID tenantId;

    @Column(name = "student_id", nullable = false)
    UUID studentId;

    @Column(name = "plan_id", nullable = false)
    UUID planId;

    @Column(name = "plan_price_id", nullable = false)
    UUID planPriceId;

    @Column(name = "branch_id", nullable = false)
    UUID branchId;

    @Column(name = "registration_number", nullable = false)
    String registrationNumber;

    @Column(nullable = false)
    String status;

    @Column(name = "start_date", nullable = false)
    LocalDate startDate;

    @Column(name = "end_date", nullable = false)
    LocalDate endDate;

    @Column(name = "original_end_date", nullable = false)
    LocalDate originalEndDate;

    @Column(name = "anchor_date", nullable = false)
    LocalDate anchorDate;

    @Column(name = "term_sequence", nullable = false)
    int termSequence;

    @Column(name = "duration_type", nullable = false)
    String durationType;

    @Column(name = "duration_value", nullable = false)
    int durationValue;

    @Column(nullable = false, precision = 12, scale = 2)
    BigDecimal amount;

    @Column(name = "setup_fee", nullable = false, precision = 12, scale = 2)
    BigDecimal setupFee;

    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(nullable = false, length = 3)
    String currency;

    @Column(name = "billing_period", nullable = false)
    String billingPeriod;

    @Column(name = "billing_interval", nullable = false)
    int billingInterval;

    @Column(name = "discount_amount", nullable = false, precision = 12, scale = 2)
    BigDecimal discountAmount;

    @Column(name = "discount_reason")
    String discountReason;

    @Column(name = "final_amount", nullable = false, precision = 12, scale = 2)
    BigDecimal finalAmount;

    @Column(name = "max_freeze_days")
    Integer maxFreezeDays;

    @Column(name = "max_freezes")
    Integer maxFreezes;

    @Column(name = "min_freeze_days")
    Integer minFreezeDays;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "conditions_snapshot", columnDefinition = "jsonb", nullable = false)
    Map<String, Object> conditionsSnapshot;

    @Column(name = "renewed_from_id")
    UUID renewedFromId;

    String notes;

    @Column(name = "cancelled_at")
    Instant cancelledAt;

    @Column(name = "cancelled_by")
    UUID cancelledBy;

    @Column(name = "cancellation_reason")
    String cancellationReason;

    @Column(name = "created_by")
    UUID createdBy;

    @Version
    @Column(nullable = false)
    int version;

    @Column(name = "created_at", nullable = false)
    Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    Instant updatedAt;

    protected StudentMembershipJpaEntity() {
    }
}
