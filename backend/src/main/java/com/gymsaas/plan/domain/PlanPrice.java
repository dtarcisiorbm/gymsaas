package com.gymsaas.plan.domain;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

/**
 * One entry of a plan's price history. Commercial fields are immutable; only
 * the validity end can be set when a newer price supersedes it.
 */
public record PlanPrice(
        UUID id,
        UUID tenantId,
        UUID planId,
        BigDecimal amount,
        String currency,
        BillingPeriod billingPeriod,
        int billingInterval,
        BigDecimal setupFee,
        LocalDate validFrom,
        LocalDate validUntil,
        PlanPriceStatus status,
        String notes,
        UUID createdBy,
        Instant createdAt) {

    public boolean effectiveOn(LocalDate date) {
        return status == PlanPriceStatus.ACTIVE
                && !validFrom.isAfter(date)
                && (validUntil == null || !validUntil.isBefore(date));
    }

    public PlanPrice closedAt(LocalDate lastValidDay) {
        return new PlanPrice(id, tenantId, planId, amount, currency, billingPeriod, billingInterval,
                setupFee, validFrom, lastValidDay, status, notes, createdBy, createdAt);
    }

    /** A scheduled price that never took effect leaves the active schedule. */
    public PlanPrice cancelled() {
        return new PlanPrice(id, tenantId, planId, amount, currency, billingPeriod, billingInterval,
                setupFee, validFrom, validUntil, PlanPriceStatus.CANCELLED, notes, createdBy, createdAt);
    }
}
