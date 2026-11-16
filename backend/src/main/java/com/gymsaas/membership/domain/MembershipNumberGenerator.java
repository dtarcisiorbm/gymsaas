package com.gymsaas.membership.domain;

import java.util.UUID;

/** Sequential membership number per tenant and year: M + yyyy + 6 digits. */
public interface MembershipNumberGenerator {

    String next(UUID tenantId, int year);

    static String format(int year, long value) {
        if (value < 1 || value > 999_999) {
            throw new IllegalArgumentException("Membership counter out of range.");
        }
        return "M%d%06d".formatted(year, value);
    }
}
