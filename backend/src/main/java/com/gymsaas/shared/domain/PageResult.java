package com.gymsaas.shared.domain;

import java.util.List;

public record PageResult<T>(List<T> content, long totalElements, int page, int size) {

    public static <T> PageResult<T> of(List<T> content, long totalElements, int page, int size) {
        return new PageResult<>(content, totalElements, page, size);
    }
}
