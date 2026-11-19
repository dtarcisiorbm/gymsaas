package com.gymsaas.billing.domain;

import java.util.UUID;

/** Sequential numbers per tenant and year: C/R + yyyy + 6 digits. */
public interface BillingNumberGenerator {

    String nextCharge(UUID tenantId, int year);

    String nextPayment(UUID tenantId, int year);

    static String format(char prefix, int year, long value) {
        if (value < 1 || value > 999_999) {
            throw new IllegalArgumentException("Billing counter out of range.");
        }
        return "%c%d%06d".formatted(prefix, year, value);
    }
}
