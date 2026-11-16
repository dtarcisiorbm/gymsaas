package com.gymsaas.plan.domain;

/**
 * Contract length of a plan. Fixed types always have duration value 1;
 * CUSTOM_MONTHS (1..120) and CUSTOM_DAYS (1..3650) carry their own value.
 */
public enum DurationType {
    MONTHLY(1),
    QUARTERLY(3),
    SEMIANNUAL(6),
    ANNUAL(12),
    CUSTOM_MONTHS(0),
    CUSTOM_DAYS(0);

    private final int fixedMonths;

    DurationType(int fixedMonths) {
        this.fixedMonths = fixedMonths;
    }

    public static DurationType parse(String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("Duration type is required.");
        }
        try {
            return DurationType.valueOf(value.trim().toUpperCase());
        } catch (IllegalArgumentException exception) {
            throw new IllegalArgumentException("Unknown duration type: " + value);
        }
    }

    public boolean custom() {
        return this == CUSTOM_MONTHS || this == CUSTOM_DAYS;
    }

    public boolean dayBased() {
        return this == CUSTOM_DAYS;
    }

    /** Months covered by one contract term; 0 for day-based plans. */
    public int monthsPerTerm(int durationValue) {
        return switch (this) {
            case CUSTOM_MONTHS -> durationValue;
            case CUSTOM_DAYS -> 0;
            default -> fixedMonths;
        };
    }

    /** Returns true when the value is acceptable for this type. */
    public boolean acceptsValue(int value) {
        return switch (this) {
            case CUSTOM_MONTHS -> value >= 1 && value <= 120;
            case CUSTOM_DAYS -> value >= 1 && value <= 3650;
            default -> value == 1;
        };
    }
}
