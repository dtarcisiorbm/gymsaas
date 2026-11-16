package com.gymsaas.membership.domain;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

/**
 * A student's membership. Commercial conditions, start date, original end
 * date and calendar anchor are immutable; only status, current end date
 * (freezes), operational fields and cancellation data change over time.
 */
public record Membership(
        UUID id,
        UUID tenantId,
        UUID studentId,
        UUID planId,
        UUID planPriceId,
        UUID branchId,
        String registrationNumber,
        MembershipStatus status,
        LocalDate startDate,
        LocalDate endDate,
        LocalDate originalEndDate,
        LocalDate anchorDate,
        int termSequence,
        ContractedConditions conditions,
        UUID renewedFromId,
        String notes,
        Instant cancelledAt,
        UUID cancelledBy,
        String cancellationReason,
        UUID createdBy,
        int version,
        Instant createdAt,
        Instant updatedAt) {

    public Membership withStatus(MembershipStatus target, Instant now) {
        return new Membership(id, tenantId, studentId, planId, planPriceId, branchId, registrationNumber, target,
                startDate, endDate, originalEndDate, anchorDate, termSequence, conditions, renewedFromId, notes,
                cancelledAt, cancelledBy, cancellationReason, createdBy, version, createdAt, now);
    }

    public Membership withEndDate(LocalDate newEndDate, MembershipStatus target, Instant now) {
        if (newEndDate.isBefore(originalEndDate)) {
            throw new IllegalArgumentException("End date cannot be earlier than the original end date.");
        }
        return new Membership(id, tenantId, studentId, planId, planPriceId, branchId, registrationNumber, target,
                startDate, newEndDate, originalEndDate, anchorDate, termSequence, conditions, renewedFromId, notes,
                cancelledAt, cancelledBy, cancellationReason, createdBy, version, createdAt, now);
    }

    public Membership withCancellation(Instant at, UUID by, String reason) {
        return new Membership(id, tenantId, studentId, planId, planPriceId, branchId, registrationNumber,
                MembershipStatus.CANCELLED, startDate, endDate, originalEndDate, anchorDate, termSequence, conditions,
                renewedFromId, notes, at, by, reason, createdBy, version, createdAt, at);
    }

    public Membership withOperational(UUID newBranchId, String newNotes, Instant now) {
        return new Membership(id, tenantId, studentId, planId, planPriceId, newBranchId, registrationNumber, status,
                startDate, endDate, originalEndDate, anchorDate, termSequence, conditions, renewedFromId, newNotes,
                cancelledAt, cancelledBy, cancellationReason, createdBy, version, createdAt, now);
    }

    /** True when freezes have shifted the end date beyond the calendar end. */
    public boolean endShiftedByFreezes() {
        return !endDate.equals(originalEndDate);
    }
}
