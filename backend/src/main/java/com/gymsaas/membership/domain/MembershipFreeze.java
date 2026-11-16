package com.gymsaas.membership.domain;

import java.time.Instant;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

/**
 * A freeze over the inclusive period [startDate, plannedEndDate]. When the
 * member resumes on date R (start &lt; R &lt;= plannedEnd) the effective
 * frozen days are R - start; resuming on the start day uses 0 days.
 */
public record MembershipFreeze(
        UUID id,
        UUID tenantId,
        UUID membershipId,
        LocalDate startDate,
        LocalDate plannedEndDate,
        LocalDate actualEndDate,
        int plannedDays,
        Integer effectiveDays,
        FreezeStatus status,
        String reason,
        UUID createdBy,
        UUID endedBy,
        Instant createdAt,
        Instant endedAt) {

    public static int plannedDays(LocalDate start, LocalDate plannedEnd) {
        return (int) ChronoUnit.DAYS.between(start, plannedEnd) + 1;
    }

    /** Days actually frozen if the member resumes on {@code resumeDate}. */
    public int effectiveDaysIfResumedOn(LocalDate resumeDate) {
        long days = ChronoUnit.DAYS.between(startDate, resumeDate);
        return (int) Math.max(0, Math.min(plannedDays, days));
    }

    /** Days counted against the policy: effective when closed, planned while active. */
    public int consumedDays() {
        return switch (status) {
            case ACTIVE -> plannedDays;
            case COMPLETED, CANCELLED -> effectiveDays == null ? 0 : effectiveDays;
        };
    }

    /** Ends the freeze so the member resumes on {@code resumeDate}. */
    public MembershipFreeze endedOn(LocalDate resumeDate, UUID by, Instant at) {
        int effective = effectiveDaysIfResumedOn(resumeDate);
        if (effective == 0) {
            return new MembershipFreeze(id, tenantId, membershipId, startDate, plannedEndDate, null, plannedDays, 0,
                    FreezeStatus.CANCELLED, reason, createdBy, by, createdAt, at);
        }
        return new MembershipFreeze(id, tenantId, membershipId, startDate, plannedEndDate,
                startDate.plusDays(effective - 1L), plannedDays, effective, FreezeStatus.COMPLETED, reason,
                createdBy, by, createdAt, at);
    }
}
