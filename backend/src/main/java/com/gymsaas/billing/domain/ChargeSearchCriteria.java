package com.gymsaas.billing.domain;

import java.time.LocalDate;
import java.util.UUID;

/**
 * {@code overdueOnly} selects OPEN charges due before {@code today}. {@code status}
 * and {@code overdueOnly} are combined with AND.
 */
public record ChargeSearchCriteria(
        String search,
        ChargeStatus status,
        boolean overdueOnly,
        LocalDate today,
        UUID studentId,
        UUID membershipId,
        UUID branchId,
        LocalDate dueFrom,
        LocalDate dueTo,
        int page,
        int size,
        String sort,
        String direction) {
}
