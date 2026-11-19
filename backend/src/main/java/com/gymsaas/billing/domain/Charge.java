package com.gymsaas.billing.domain;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Objects;
import java.util.UUID;

/**
 * A receivable of a student. Amount, due date and identity are immutable
 * (trigger in V13); only the status and its audit fields change.
 */
public record Charge(
        UUID id,
        UUID tenantId,
        String chargeNumber,
        UUID studentId,
        UUID membershipId,
        UUID branchId,
        ChargeKind kind,
        Integer installmentNumber,
        Integer installmentCount,
        String description,
        BigDecimal amount,
        String currency,
        LocalDate dueDate,
        LocalDate periodStart,
        LocalDate periodEnd,
        ChargeStatus status,
        Instant paidAt,
        Instant cancelledAt,
        UUID cancelledBy,
        String cancellationReason,
        String notes,
        UUID createdBy,
        Instant createdAt,
        Instant updatedAt) {

    public Charge {
        Objects.requireNonNull(amount, "amount");
        Objects.requireNonNull(dueDate, "dueDate");
        Objects.requireNonNull(kind, "kind");
        Objects.requireNonNull(status, "status");
        if (amount.signum() <= 0) {
            throw new IllegalArgumentException("Charge amount must be positive.");
        }
        if (kind != ChargeKind.MANUAL && membershipId == null) {
            throw new IllegalArgumentException("Membership charges require a membership.");
        }
    }

    /** OPEN and due before the tenant's today. */
    public boolean overdue(LocalDate today) {
        return status == ChargeStatus.OPEN && dueDate.isBefore(today);
    }

    public Charge paid(Instant at) {
        requireStatus(ChargeStatus.OPEN, "Only open charges can be paid.");
        return new Charge(id, tenantId, chargeNumber, studentId, membershipId, branchId, kind, installmentNumber,
                installmentCount, description, amount, currency, dueDate, periodStart, periodEnd, ChargeStatus.PAID,
                at, null, null, null, notes, createdBy, createdAt, at);
    }

    /** Refund of the settling payment: the charge is open again. */
    public Charge reopened(Instant at) {
        requireStatus(ChargeStatus.PAID, "Only paid charges can be reopened.");
        return new Charge(id, tenantId, chargeNumber, studentId, membershipId, branchId, kind, installmentNumber,
                installmentCount, description, amount, currency, dueDate, periodStart, periodEnd, ChargeStatus.OPEN,
                null, null, null, null, notes, createdBy, createdAt, at);
    }

    public Charge cancelled(Instant at, UUID by, String reason) {
        requireStatus(ChargeStatus.OPEN, "Only open charges can be cancelled.");
        if (reason == null || reason.isBlank()) {
            throw new IllegalArgumentException("Cancellation requires a reason.");
        }
        return new Charge(id, tenantId, chargeNumber, studentId, membershipId, branchId, kind, installmentNumber,
                installmentCount, description, amount, currency, dueDate, periodStart, periodEnd,
                ChargeStatus.CANCELLED, null, at, by, reason.trim(), notes, createdBy, createdAt, at);
    }

    private void requireStatus(ChargeStatus expected, String message) {
        if (status != expected) {
            throw new IllegalStateException(message);
        }
    }
}
