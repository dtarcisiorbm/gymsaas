package com.gymsaas.membership.domain;

/** ACTIVE running; COMPLETED ended (naturally or early); CANCELLED ended on its first day (0 days used). */
public enum FreezeStatus {
    ACTIVE,
    COMPLETED,
    CANCELLED
}
