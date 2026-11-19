package com.gymsaas.billing.domain;

/** How the operator received the money (no gateway in this phase). */
public enum PaymentMethod {
    CASH,
    PIX,
    CREDIT_CARD,
    DEBIT_CARD,
    BANK_TRANSFER,
    OTHER;

    public static PaymentMethod parse(String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("Payment method is required.");
        }
        return PaymentMethod.valueOf(value.trim().toUpperCase());
    }
}
