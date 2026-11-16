package com.gymsaas.plan.domain;

/**
 * Billing frequency of a price (independent from the contract length).
 * ONE_TIME means a single charge for the whole contract term.
 */
public enum BillingPeriod {
    MONTHLY(1),
    QUARTERLY(3),
    SEMIANNUAL(6),
    ANNUAL(12),
    ONE_TIME(0);

    private final int months;

    BillingPeriod(int months) {
        this.months = months;
    }

    public static BillingPeriod parse(String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("Billing period is required.");
        }
        try {
            return BillingPeriod.valueOf(value.trim().toUpperCase());
        } catch (IllegalArgumentException exception) {
            throw new IllegalArgumentException("Unknown billing period: " + value);
        }
    }

    public int months() {
        return months;
    }

    /**
     * A recurring billing cycle cannot be longer than the contract term. Day
     * based contracts only accept ONE_TIME billing.
     */
    public boolean compatibleWith(DurationType durationType, int durationValue, int interval) {
        if (this == ONE_TIME) {
            return interval == 1;
        }
        if (durationType.dayBased()) {
            return false;
        }
        return months * interval <= durationType.monthsPerTerm(durationValue);
    }
}
