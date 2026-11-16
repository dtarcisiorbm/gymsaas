package com.gymsaas.shared.infrastructure.web;

import java.time.Instant;
import java.util.Map;

public record ErrorResponse(
        Instant timestamp,
        int status,
        String code,
        String message,
        String path,
        Map<String, Object> details) {

    public static ErrorResponse of(int status, String code, String message, String path) {
        return new ErrorResponse(Instant.now(), status, code, message, path, null);
    }

    public static ErrorResponse of(int status, String code, String message, String path,
                                   Map<String, Object> details) {
        return new ErrorResponse(Instant.now(), status, code, message, path, details);
    }
}
