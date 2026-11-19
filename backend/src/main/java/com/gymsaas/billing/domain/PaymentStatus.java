package com.gymsaas.billing.domain;

/** CONFIRMED -> REFUNDED is the only transition; rows are never deleted. */
public enum PaymentStatus {
    CONFIRMED,
    REFUNDED
}
