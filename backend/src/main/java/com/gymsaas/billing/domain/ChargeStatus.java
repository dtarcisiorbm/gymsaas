package com.gymsaas.billing.domain;

/**
 * OPEN -> PAID | CANCELLED, PAID -> OPEN (refund). "Overdue" is not a stored
 * status: it is an OPEN charge whose due date is before the tenant's today.
 */
public enum ChargeStatus {
    OPEN,
    PAID,
    CANCELLED;

    public static ChargeStatus parse(String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("Status is required.");
        }
        return ChargeStatus.valueOf(value.trim().toUpperCase());
    }
}
