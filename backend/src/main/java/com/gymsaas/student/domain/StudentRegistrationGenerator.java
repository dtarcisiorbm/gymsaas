package com.gymsaas.student.domain;

import java.util.UUID;

public interface StudentRegistrationGenerator {

    /**
     * Generates the next registration number for the tenant. The year prefix
     * keeps numbers readable and the sequence restarts every year.
     */
    String next(UUID tenantId);

    static String format(int year, long value) {
        return String.format("%d%05d", year, value);
    }
}
