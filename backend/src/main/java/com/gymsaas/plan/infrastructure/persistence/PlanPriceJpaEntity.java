package com.gymsaas.plan.infrastructure.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "plan_prices")
public class PlanPriceJpaEntity {

    @Id
    UUID id;

    @Column(name = "tenant_id", nullable = false)
    UUID tenantId;

    @Column(name = "plan_id", nullable = false)
    UUID planId;

    @Column(nullable = false, precision = 12, scale = 2)
    BigDecimal amount;

    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(nullable = false, length = 3)
    String currency;

    @Column(name = "billing_period", nullable = false)
    String billingPeriod;

    @Column(name = "billing_interval", nullable = false)
    int billingInterval;

    @Column(name = "setup_fee", nullable = false, precision = 12, scale = 2)
    BigDecimal setupFee;

    @Column(name = "valid_from", nullable = false)
    LocalDate validFrom;

    @Column(name = "valid_until")
    LocalDate validUntil;

    @Column(nullable = false)
    String status;

    String notes;

    @Column(name = "created_by")
    UUID createdBy;

    @Column(name = "created_at", nullable = false)
    Instant createdAt;

    protected PlanPriceJpaEntity() {
    }
}
