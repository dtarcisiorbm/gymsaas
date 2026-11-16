package com.gymsaas.membership.domain;

import com.gymsaas.plan.domain.BillingPeriod;
import com.gymsaas.plan.domain.DurationType;
import com.gymsaas.plan.domain.FreezePolicy;
import com.gymsaas.plan.domain.PlanFeature;
import com.gymsaas.plan.domain.PlanFeatureType;
import com.gymsaas.plan.domain.PlanStatus;
import com.gymsaas.shared.domain.Money;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MembershipRulesTest {

    @Test
    void membershipStatusTransitions() {
        assertTrue(MembershipStatus.PENDING.canTransitionTo(MembershipStatus.ACTIVE));
        assertTrue(MembershipStatus.ACTIVE.canTransitionTo(MembershipStatus.FROZEN));
        assertTrue(MembershipStatus.FROZEN.canTransitionTo(MembershipStatus.ACTIVE));
        assertTrue(MembershipStatus.SUSPENDED.canTransitionTo(MembershipStatus.ACTIVE));
        assertFalse(MembershipStatus.PENDING.canTransitionTo(MembershipStatus.FROZEN));
        assertFalse(MembershipStatus.CANCELLED.canTransitionTo(MembershipStatus.ACTIVE));
        assertFalse(MembershipStatus.EXPIRED.canTransitionTo(MembershipStatus.ACTIVE));
        assertTrue(MembershipStatus.FROZEN.open());
        assertFalse(MembershipStatus.EXPIRED.open());
    }

    @Test
    void planStatusTransitions() {
        assertTrue(PlanStatus.ACTIVE.canTransitionTo(PlanStatus.INACTIVE));
        assertTrue(PlanStatus.ARCHIVED.canTransitionTo(PlanStatus.INACTIVE));
        assertFalse(PlanStatus.ARCHIVED.canTransitionTo(PlanStatus.ACTIVE));
    }

    @Test
    void durationValuesAreValidatedPerType() {
        assertTrue(DurationType.MONTHLY.acceptsValue(1));
        assertFalse(DurationType.MONTHLY.acceptsValue(2));
        assertTrue(DurationType.CUSTOM_MONTHS.acceptsValue(120));
        assertFalse(DurationType.CUSTOM_MONTHS.acceptsValue(121));
        assertTrue(DurationType.CUSTOM_DAYS.acceptsValue(3650));
        assertFalse(DurationType.CUSTOM_DAYS.acceptsValue(0));
    }

    @Test
    void billingFrequencyIsIndependentButBoundedByContractLength() {
        assertTrue(BillingPeriod.MONTHLY.compatibleWith(DurationType.ANNUAL, 1, 1));
        assertTrue(BillingPeriod.ANNUAL.compatibleWith(DurationType.ANNUAL, 1, 1));
        assertFalse(BillingPeriod.ANNUAL.compatibleWith(DurationType.MONTHLY, 1, 1));
        assertFalse(BillingPeriod.MONTHLY.compatibleWith(DurationType.CUSTOM_DAYS, 30, 1));
        assertTrue(BillingPeriod.ONE_TIME.compatibleWith(DurationType.CUSTOM_DAYS, 30, 1));
        assertFalse(BillingPeriod.ONE_TIME.compatibleWith(DurationType.ANNUAL, 1, 2));
        assertFalse(BillingPeriod.QUARTERLY.compatibleWith(DurationType.SEMIANNUAL, 1, 3));
    }

    @Test
    void freezePolicyLimits() {
        FreezePolicy policy = new FreezePolicy(30, 2, 7);
        assertNull(policy.freezeViolation(0, 0, 7));
        assertEquals("FREEZE_TOO_SHORT", policy.freezeViolation(0, 0, 6));
        assertEquals("FREEZE_LIMIT_REACHED", policy.freezeViolation(2, 14, 7));
        assertEquals("FREEZE_DAYS_EXCEEDED", policy.freezeViolation(1, 25, 7));
        assertEquals("FREEZE_NOT_ALLOWED", new FreezePolicy(null, 0, null).freezeViolation(0, 0, 10));
        assertNull(FreezePolicy.unlimited().freezeViolation(50, 500, 365));
        assertEquals("INVALID_FREEZE_POLICY", new FreezePolicy(5, null, 10).configurationViolation());
    }

    @Test
    void earlyUnfreezeCountsOnlyUsedDays() {
        MembershipFreeze freeze = freeze("2027-03-10", "2027-03-19");
        assertEquals(10, freeze.plannedDays());

        MembershipFreeze early = freeze.endedOn(LocalDate.parse("2027-03-14"), null, Instant.EPOCH);
        assertEquals(FreezeStatus.COMPLETED, early.status());
        assertEquals(4, early.effectiveDays());
        assertEquals(LocalDate.parse("2027-03-13"), early.actualEndDate());

        MembershipFreeze sameDay = freeze.endedOn(LocalDate.parse("2027-03-10"), null, Instant.EPOCH);
        assertEquals(FreezeStatus.CANCELLED, sameDay.status());
        assertEquals(0, sameDay.effectiveDays());
        assertNull(sameDay.actualEndDate());

        MembershipFreeze late = freeze.endedOn(LocalDate.parse("2027-04-30"), null, Instant.EPOCH);
        assertEquals(10, late.effectiveDays());
        assertEquals(LocalDate.parse("2027-03-19"), late.actualEndDate());
    }

    @Test
    void contractedConditionsEnforceDiscountInvariants() {
        BigDecimal amount = new BigDecimal("100.00");
        assertThrows(IllegalArgumentException.class, () -> conditions(amount, new BigDecimal("100.01"), "x",
                new BigDecimal("-0.01")));
        assertThrows(IllegalArgumentException.class, () -> conditions(amount, new BigDecimal("10.00"), null,
                new BigDecimal("90.00")));
        assertThrows(IllegalArgumentException.class, () -> conditions(amount, new BigDecimal("10.00"), "promo",
                new BigDecimal("91.00")));
        assertNotNull(conditions(amount, new BigDecimal("10.00"), "promo", new BigDecimal("90.00")));
    }

    @Test
    void endDateNeverBeforeOriginalEnd() {
        Membership membership = MembershipTermTest.membership(
                MembershipTerm.first(LocalDate.parse("2027-01-15"), DurationType.MONTHLY, 1),
                LocalDate.parse("2027-02-14"), DurationType.MONTHLY, 1);
        assertThrows(IllegalArgumentException.class,
                () -> membership.withEndDate(LocalDate.parse("2027-02-13"), MembershipStatus.ACTIVE, Instant.EPOCH));
    }

    @Test
    void featureShapesAreTyped() {
        UUID branch = UUID.randomUUID();
        assertNull(PlanFeatureType.BRANCH_ACCESS.shapeViolation(feature(PlanFeatureType.BRANCH_ACCESS, branch, null,
                null, null, null, null)));
        assertNotNull(PlanFeatureType.BRANCH_ACCESS.shapeViolation(feature(PlanFeatureType.BRANCH_ACCESS, null, null,
                null, null, null, null)));
        assertNull(PlanFeatureType.TIME_WINDOW.shapeViolation(feature(PlanFeatureType.TIME_WINDOW, null, null,
                LocalTime.of(6, 0), LocalTime.of(12, 0), 31, null)));
        assertNotNull(PlanFeatureType.TIME_WINDOW.shapeViolation(feature(PlanFeatureType.TIME_WINDOW, null, null,
                LocalTime.of(12, 0), LocalTime.of(6, 0), 31, null)));
        assertNotNull(PlanFeatureType.WEEKLY_CHECKIN_LIMIT.shapeViolation(feature(
                PlanFeatureType.WEEKLY_CHECKIN_LIMIT, null, null, null, null, null, null)));
        assertNotNull(PlanFeatureType.CUSTOM.shapeViolation(feature(PlanFeatureType.CUSTOM, null, null, null, null,
                null, " ")));
    }

    @Test
    void membershipNumberFormat() {
        assertEquals("M2027000042", MembershipNumberGenerator.format(2027, 42));
        assertThrows(IllegalArgumentException.class, () -> MembershipNumberGenerator.format(2027, 1_000_000));
    }

    private static ContractedConditions conditions(BigDecimal amount, BigDecimal discount, String reason,
                                                   BigDecimal finalAmount) {
        return new ContractedConditions(DurationType.MONTHLY, 1, amount, Money.zero(), "BRL", BillingPeriod.MONTHLY,
                1, discount, reason, finalAmount, FreezePolicy.unlimited(), Map.of());
    }

    private static MembershipFreeze freeze(String start, String plannedEnd) {
        LocalDate startDate = LocalDate.parse(start);
        LocalDate endDate = LocalDate.parse(plannedEnd);
        return new MembershipFreeze(UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), startDate, endDate, null,
                MembershipFreeze.plannedDays(startDate, endDate), null, FreezeStatus.ACTIVE, "viagem", null, null,
                Instant.EPOCH, null);
    }

    private static PlanFeature feature(PlanFeatureType type, UUID branchId, Integer intValue, LocalTime start,
                                       LocalTime end, Integer days, String label) {
        return new PlanFeature(UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), type, branchId, intValue,
                start, end, days, label, null, 0, Instant.EPOCH);
    }
}
