package com.gymsaas.membership.domain;

import java.time.LocalDate;
import java.util.UUID;

public record MembershipSearchCriteria(
        String search,
        UUID studentId,
        UUID planId,
        UUID branchId,
        MembershipStatus status,
        LocalDate endingFrom,
        LocalDate endingTo,
        int page,
        int size,
        String sort,
        String direction) {
}
