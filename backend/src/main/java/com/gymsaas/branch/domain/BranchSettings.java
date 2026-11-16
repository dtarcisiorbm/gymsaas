package com.gymsaas.branch.domain;

import java.time.LocalTime;
import java.util.Map;
import java.util.UUID;

public record BranchSettings(
        UUID id,
        UUID tenantId,
        UUID branchId,
        String timezone,
        LocalTime openingTime,
        LocalTime closingTime,
        Integer maxCapacity,
        boolean allow24hAccess,
        Map<String, Object> settings) {
}
