package com.gymsaas.billing.domain;

import com.gymsaas.billing.domain.BillingSchedule.Installment;
import com.gymsaas.membership.domain.MembershipTerm;
import com.gymsaas.plan.domain.BillingPeriod;
import com.gymsaas.plan.domain.DurationType;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class BillingScheduleTest {

    private static List<Installment> schedule(LocalDate start, DurationType type, int value, BillingPeriod period,
                                              int interval) {
        LocalDate end = MembershipTerm.first(start, type, value).endDate();
        return BillingSchedule.installments(start, end, type, value, period, interval);
    }

    @Test
    void monthlyPlanBilledMonthlyHasOneInstallmentCoveringTheTerm() {
        List<Installment> result = schedule(LocalDate.of(2026, 3, 10), DurationType.MONTHLY, 1,
                BillingPeriod.MONTHLY, 1);
        assertEquals(List.of(new Installment(1, 1, LocalDate.of(2026, 3, 10), LocalDate.of(2026, 3, 10),
                LocalDate.of(2026, 4, 9))), result);
    }

    @Test
    void annualPlanBilledMonthlyHasTwelveInstallments() {
        List<Installment> result = schedule(LocalDate.of(2026, 1, 15), DurationType.ANNUAL, 1,
                BillingPeriod.MONTHLY, 1);
        assertEquals(12, result.size());
        assertEquals(LocalDate.of(2026, 1, 15), result.get(0).dueDate());
        assertEquals(LocalDate.of(2026, 2, 14), result.get(0).periodEnd());
        assertEquals(LocalDate.of(2026, 12, 15), result.get(11).dueDate());
        assertEquals(LocalDate.of(2027, 1, 14), result.get(11).periodEnd());
        assertEquals(12, result.get(11).number());
        result.forEach(installment -> assertEquals(12, installment.count()));
    }

    @Test
    void annualPlanBilledQuarterlyHasFourInstallments() {
        List<Installment> result = schedule(LocalDate.of(2026, 5, 1), DurationType.ANNUAL, 1,
                BillingPeriod.QUARTERLY, 1);
        assertEquals(List.of(LocalDate.of(2026, 5, 1), LocalDate.of(2026, 8, 1), LocalDate.of(2026, 11, 1),
                LocalDate.of(2027, 2, 1)), result.stream().map(Installment::dueDate).toList());
        assertEquals(LocalDate.of(2026, 7, 31), result.get(0).periodEnd());
        assertEquals(LocalDate.of(2027, 4, 30), result.get(3).periodEnd());
    }

    @Test
    void intervalMultipliesTheCycleAndRoundsUp() {
        // 12 months billed every 5 months -> ceil(12/5) = 3 installments; the last one is shorter.
        List<Installment> result = schedule(LocalDate.of(2026, 1, 1), DurationType.ANNUAL, 1,
                BillingPeriod.MONTHLY, 5);
        assertEquals(3, result.size());
        assertEquals(LocalDate.of(2026, 6, 1), result.get(1).dueDate());
        assertEquals(LocalDate.of(2026, 11, 1), result.get(2).dueDate());
        assertEquals(LocalDate.of(2026, 12, 31), result.get(2).periodEnd());
    }

    @Test
    void oneTimeAndDayBasedPlansHaveSingleInstallment() {
        List<Installment> oneTime = schedule(LocalDate.of(2026, 2, 1), DurationType.SEMIANNUAL, 1,
                BillingPeriod.ONE_TIME, 1);
        assertEquals(1, oneTime.size());
        assertEquals(LocalDate.of(2026, 7, 31), oneTime.get(0).periodEnd());

        List<Installment> days = schedule(LocalDate.of(2026, 2, 1), DurationType.CUSTOM_DAYS, 10,
                BillingPeriod.ONE_TIME, 1);
        assertEquals(List.of(new Installment(1, 1, LocalDate.of(2026, 2, 1), LocalDate.of(2026, 2, 1),
                LocalDate.of(2026, 2, 10))), days);
    }

    @Test
    void dayThirtyOneClampsToEndOfMonthWithoutDrift() {
        List<Installment> result = schedule(LocalDate.of(2026, 1, 31), DurationType.SEMIANNUAL, 1,
                BillingPeriod.MONTHLY, 1);
        assertEquals(List.of(LocalDate.of(2026, 1, 31), LocalDate.of(2026, 2, 28), LocalDate.of(2026, 3, 31),
                LocalDate.of(2026, 4, 30), LocalDate.of(2026, 5, 31), LocalDate.of(2026, 6, 30)),
                result.stream().map(Installment::dueDate).toList());
        assertEquals(LocalDate.of(2026, 2, 27), result.get(0).periodEnd());
        assertEquals(LocalDate.of(2026, 3, 30), result.get(1).periodEnd());
    }

    @Test
    void leapYearFebruary() {
        List<Installment> result = schedule(LocalDate.of(2028, 1, 29), DurationType.QUARTERLY, 1,
                BillingPeriod.MONTHLY, 1);
        assertEquals(LocalDate.of(2028, 2, 29), result.get(1).dueDate());
        assertEquals(LocalDate.of(2028, 3, 29), result.get(2).dueDate());

        List<Installment> fromLeapDay = schedule(LocalDate.of(2028, 2, 29), DurationType.ANNUAL, 1,
                BillingPeriod.ANNUAL, 1);
        assertEquals(1, fromLeapDay.size());
        assertEquals(LocalDate.of(2029, 2, 27), fromLeapDay.get(0).periodEnd());
    }

    @Test
    void customMonthsPlan() {
        List<Installment> result = schedule(LocalDate.of(2026, 1, 10), DurationType.CUSTOM_MONTHS, 7,
                BillingPeriod.QUARTERLY, 1);
        assertEquals(3, result.size());
        assertEquals(LocalDate.of(2026, 8, 9), result.get(2).periodEnd());
    }

    @Test
    void rejectsInvalidTerm() {
        assertThrows(IllegalArgumentException.class, () -> BillingSchedule.installments(LocalDate.of(2026, 2, 1),
                LocalDate.of(2026, 1, 1), DurationType.MONTHLY, 1, BillingPeriod.MONTHLY, 1));
    }
}
