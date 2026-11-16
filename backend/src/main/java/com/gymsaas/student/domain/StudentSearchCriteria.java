package com.gymsaas.student.domain;

import java.util.UUID;

public record StudentSearchCriteria(
        String search,
        String document,
        StudentStatus status,
        UUID branchId,
        int page,
        int size,
        String sort,
        String direction) {
}
