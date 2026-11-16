package com.gymsaas.plan.domain;

public record PlanSearchCriteria(
        String search,
        PlanStatus status,
        int page,
        int size,
        String sort,
        String direction) {
}
