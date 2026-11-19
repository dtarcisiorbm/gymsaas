package com.gymsaas.billing.domain;

import com.gymsaas.plan.domain.BillingPeriod;
import com.gymsaas.plan.domain.DurationType;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * Installment schedule of a membership term (decision F2).
 *
 * <p>{@code cycle = months(billingPeriod) x interval}; {@code count = ceil(termMonths / cycle)}.
 * ONE_TIME prices and day-based plans produce a single installment. The due
 * date of installment {@code i} (0-based) is {@code start + i x cycle} months,
 * always derived from the start (no drift); {@link LocalDate#plusMonths(long)}
 * clamps the 31st to the last day of shorter months. Each installment covers
 * {@code [due, nextDue - 1]}, the last one clamped to the term end.</p>
 */
public final class BillingSchedule {

    private BillingSchedule() {
    }

    public record Installment(int number, int count, LocalDate dueDate, LocalDate periodStart, LocalDate periodEnd) {
    }

    public static List<Installment> installments(LocalDate start, LocalDate end, DurationType durationType,
                                                 int durationValue, BillingPeriod period, int interval) {
        if (start == null || end == null || end.isBefore(start)) {
            throw new IllegalArgumentException("Invalid term.");
        }
        int termMonths = durationType.monthsPerTerm(durationValue);
        if (period == BillingPeriod.ONE_TIME || durationType.dayBased() || termMonths <= 0) {
            return List.of(new Installment(1, 1, start, start, end));
        }
        if (interval < 1) {
            throw new IllegalArgumentException("Billing interval starts at 1.");
        }
        int cycle = period.months() * interval;
        int count = (termMonths + cycle - 1) / cycle;
        List<Installment> result = new ArrayList<>(count);
        for (int i = 0; i < count; i++) {
            LocalDate due = start.plusMonths((long) i * cycle);
            LocalDate periodEnd = i == count - 1 ? end : start.plusMonths((long) (i + 1) * cycle).minusDays(1);
            if (periodEnd.isAfter(end)) {
                periodEnd = end;
            }
            if (periodEnd.isBefore(due)) {
                periodEnd = due;
            }
            result.add(new Installment(i + 1, count, due, due, periodEnd));
        }
        return List.copyOf(result);
    }
}
