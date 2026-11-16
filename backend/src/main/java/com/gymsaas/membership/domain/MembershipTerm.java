package com.gymsaas.membership.domain;

import com.gymsaas.plan.domain.DurationType;

import java.time.LocalDate;

/**
 * Contract term calculation (decisions D1/D2 of the Phase 5 plan).
 *
 * <p>Month based plans: {@code end = anchor + (months x termSequence) - 1 day}.
 * {@link LocalDate#plusMonths(long)} clamps to the last valid day of the target
 * month, so a contract started on Jan 31 ends on Feb 27 (Feb 28 - 1) and an
 * annual contract started on Feb 29, 2028 ends on Feb 27, 2029.</p>
 *
 * <p>Because the end is always derived from the original anchor (never from
 * the previous end), consecutive renewals do not drift: Jan 31 -> Feb 27,
 * Feb 28 -> Mar 30, Mar 31 -> Apr 29 ...</p>
 *
 * <p>Day based plans: {@code end = start + days - 1}.</p>
 */
public final class MembershipTerm {

    private MembershipTerm() {
    }

    public record Period(LocalDate startDate, LocalDate endDate, LocalDate anchorDate, int termSequence) {
    }

    public static LocalDate endDate(LocalDate anchor, int termSequence, LocalDate start,
                                    DurationType type, int durationValue) {
        if (termSequence < 1) {
            throw new IllegalArgumentException("Term sequence starts at 1.");
        }
        if (type.dayBased()) {
            return start.plusDays(durationValue - 1L);
        }
        long months = (long) type.monthsPerTerm(durationValue) * termSequence;
        return anchor.plusMonths(months).minusDays(1);
    }

    public static Period first(LocalDate start, DurationType type, int durationValue) {
        return new Period(start, endDate(start, 1, start, type, durationValue), start, 1);
    }

    /**
     * Renewal term. The calendar anchor is kept only for a continuous renewal
     * (starts the day after the previous end), when freezes did not shift the
     * previous end and the duration is unchanged; otherwise a new anchor
     * starts at the renewal start date.
     */
    public static Period renewal(Membership previous, LocalDate start, DurationType type, int durationValue) {
        boolean continuous = start.equals(previous.endDate().plusDays(1))
                && !previous.endShiftedByFreezes()
                && !type.dayBased()
                && type == previous.conditions().durationType()
                && durationValue == previous.conditions().durationValue();
        if (!continuous) {
            return first(start, type, durationValue);
        }
        int sequence = previous.termSequence() + 1;
        return new Period(start, endDate(previous.anchorDate(), sequence, start, type, durationValue),
                previous.anchorDate(), sequence);
    }
}
