package com.gymsaas.membership.domain;

import com.gymsaas.plan.domain.BillingPeriod;
import com.gymsaas.plan.domain.DurationType;
import com.gymsaas.plan.domain.FreezePolicy;
import com.gymsaas.shared.domain.Money;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;

/** Decisions D1/D2: end = anchor + months x sequence - 1 day, no drift across renewals. */
class MembershipTermTest {

    @Test
    void monthlyFromMidMonth() {
        assertEquals(LocalDate.of(2027, 2, 14), first("2027-01-15", DurationType.MONTHLY, 1).endDate());
    }

    @Test
    void monthlyStartedOnDay31EndsBeforeShortFebruary() {
        // 31/01 + 1 month = 28/02 (clamped) - 1 day = 27/02
        assertEquals(LocalDate.of(2027, 2, 27), first("2027-01-31", DurationType.MONTHLY, 1).endDate());
    }

    @Test
    void monthlyStartedOnDay31InLeapYearUsesFebruary29() {
        assertEquals(LocalDate.of(2028, 2, 28), first("2028-01-31", DurationType.MONTHLY, 1).endDate());
    }

    @Test
    void monthlyStartedOnDay30() {
        assertEquals(LocalDate.of(2027, 5, 29), first("2027-04-30", DurationType.MONTHLY, 1).endDate());
    }

    @Test
    void quarterlyFromDay31() {
        assertEquals(LocalDate.of(2027, 4, 29), first("2027-01-31", DurationType.QUARTERLY, 1).endDate());
    }

    @Test
    void semiannualFromDay31() {
        assertEquals(LocalDate.of(2027, 9, 29), first("2027-03-31", DurationType.SEMIANNUAL, 1).endDate());
    }

    @Test
    void annualRegular() {
        assertEquals(LocalDate.of(2028, 6, 9), first("2027-06-10", DurationType.ANNUAL, 1).endDate());
    }

    @Test
    void annualStartedOnLeapDay() {
        // 29/02/2028 + 12 months = 28/02/2029 (clamped) - 1 day = 27/02/2029
        assertEquals(LocalDate.of(2029, 2, 27), first("2028-02-29", DurationType.ANNUAL, 1).endDate());
    }

    @Test
    void annualCrossingLeapDay() {
        assertEquals(LocalDate.of(2028, 2, 29), first("2027-03-01", DurationType.ANNUAL, 1).endDate());
    }

    @Test
    void customMonths() {
        assertEquals(LocalDate.of(2027, 3, 30), first("2027-01-31", DurationType.CUSTOM_MONTHS, 2).endDate());
        assertEquals(LocalDate.of(2029, 1, 14), first("2027-01-15", DurationType.CUSTOM_MONTHS, 24).endDate());
    }

    @Test
    void customDaysIncludesLeapDay() {
        assertEquals(LocalDate.of(2027, 3, 21), first("2027-02-20", DurationType.CUSTOM_DAYS, 30).endDate());
        assertEquals(LocalDate.of(2028, 2, 29), first("2028-02-20", DurationType.CUSTOM_DAYS, 10).endDate());
        assertEquals(LocalDate.of(2027, 2, 20), first("2027-02-20", DurationType.CUSTOM_DAYS, 1).endDate());
    }

    @Test
    void consecutiveRenewalsKeepTheCalendarAnchorWithoutDrift() {
        List<LocalDate> starts = new ArrayList<>();
        List<LocalDate> ends = new ArrayList<>();
        MembershipTerm.Period period = first("2027-01-31", DurationType.MONTHLY, 1);
        Membership current = membership(period, period.endDate(), DurationType.MONTHLY, 1);
        starts.add(period.startDate());
        ends.add(period.endDate());
        for (int renewal = 0; renewal < 4; renewal++) {
            period = MembershipTerm.renewal(current, current.endDate().plusDays(1), DurationType.MONTHLY, 1);
            current = membership(period, period.endDate(), DurationType.MONTHLY, 1);
            starts.add(period.startDate());
            ends.add(period.endDate());
            assertEquals(LocalDate.of(2027, 1, 31), period.anchorDate());
        }
        assertEquals(List.of(date("2027-01-31"), date("2027-02-28"), date("2027-03-31"), date("2027-04-30"),
                date("2027-05-31")), starts);
        assertEquals(List.of(date("2027-02-27"), date("2027-03-30"), date("2027-04-29"), date("2027-05-30"),
                date("2027-06-29")), ends);
    }

    @Test
    void renewalAfterFreezeShiftStartsNewAnchor() {
        MembershipTerm.Period period = first("2027-01-31", DurationType.MONTHLY, 1);
        // Frozen for 5 days: end moved from 27/02 to 04/03.
        Membership frozenBefore = membership(period, date("2027-03-04"), DurationType.MONTHLY, 1);
        MembershipTerm.Period renewal = MembershipTerm.renewal(frozenBefore, date("2027-03-05"),
                DurationType.MONTHLY, 1);
        assertEquals(date("2027-03-05"), renewal.anchorDate());
        assertEquals(1, renewal.termSequence());
        assertEquals(date("2027-04-04"), renewal.endDate());
    }

    @Test
    void renewalWithGapStartsNewAnchor() {
        MembershipTerm.Period period = first("2027-01-15", DurationType.MONTHLY, 1);
        Membership expired = membership(period, period.endDate(), DurationType.MONTHLY, 1);
        MembershipTerm.Period renewal = MembershipTerm.renewal(expired, date("2027-03-01"), DurationType.MONTHLY, 1);
        assertEquals(date("2027-03-01"), renewal.anchorDate());
        assertEquals(date("2027-03-31"), renewal.endDate());
    }

    @Test
    void renewalWithChangedDurationStartsNewAnchor() {
        MembershipTerm.Period period = first("2027-01-31", DurationType.MONTHLY, 1);
        Membership previous = membership(period, period.endDate(), DurationType.MONTHLY, 1);
        MembershipTerm.Period renewal = MembershipTerm.renewal(previous, date("2027-02-28"),
                DurationType.QUARTERLY, 1);
        assertEquals(date("2027-02-28"), renewal.anchorDate());
        assertEquals(date("2027-05-27"), renewal.endDate());
    }

    private static MembershipTerm.Period first(String start, DurationType type, int value) {
        return MembershipTerm.first(date(start), type, value);
    }

    private static LocalDate date(String value) {
        return LocalDate.parse(value);
    }

    static Membership membership(MembershipTerm.Period period, LocalDate currentEnd, DurationType type, int value) {
        ContractedConditions conditions = new ContractedConditions(type, value, new BigDecimal("100.00"), Money.zero(),
                "BRL", BillingPeriod.MONTHLY, 1, Money.zero(), null, new BigDecimal("100.00"),
                FreezePolicy.unlimited(), Map.of());
        return new Membership(UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(),
                UUID.randomUUID(), UUID.randomUUID(), "M2027000001", MembershipStatus.ACTIVE, period.startDate(),
                currentEnd, period.endDate(), period.anchorDate(), period.termSequence(), conditions, null, null, null,
                null, null, null, 0, Instant.EPOCH, Instant.EPOCH);
    }
}
