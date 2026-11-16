package com.gymsaas.shared.domain;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Currency;
import java.util.Objects;

/**
 * Central money policy: amounts are {@link BigDecimal} with scale 2, ISO-4217
 * currency codes, BRL by default. Inputs with more than two decimal places are
 * rejected instead of being silently rounded; calculated values are rounded
 * with {@link #ROUNDING}. Never use double/float for money.
 */
public final class Money {

    public static final String DEFAULT_CURRENCY = "BRL";
    public static final int SCALE = 2;
    public static final RoundingMode ROUNDING = RoundingMode.HALF_EVEN;
    public static final BigDecimal MAX_AMOUNT = new BigDecimal("9999999999.99");

    private Money() {
    }

    public static BigDecimal zero() {
        return BigDecimal.ZERO.setScale(SCALE);
    }

    /** Validates a user/stored amount: non-null, non-negative, at most 2 decimals. */
    public static BigDecimal normalize(BigDecimal value) {
        Objects.requireNonNull(value, "amount");
        if (value.signum() < 0) {
            throw new IllegalArgumentException("Amount must not be negative.");
        }
        BigDecimal stripped = value.stripTrailingZeros();
        if (stripped.scale() > SCALE) {
            throw new IllegalArgumentException("Amount must have at most " + SCALE + " decimal places.");
        }
        BigDecimal normalized = value.setScale(SCALE, RoundingMode.UNNECESSARY);
        if (normalized.compareTo(MAX_AMOUNT) > 0) {
            throw new IllegalArgumentException("Amount exceeds the supported maximum.");
        }
        return normalized;
    }

    public static BigDecimal normalizeOrZero(BigDecimal value) {
        return value == null ? zero() : normalize(value);
    }

    /** Rounds a calculated value to the money scale using the central rounding mode. */
    public static BigDecimal round(BigDecimal value) {
        return Objects.requireNonNull(value, "amount").setScale(SCALE, ROUNDING);
    }

    public static String currency(String code) {
        if (code == null || code.isBlank()) {
            return DEFAULT_CURRENCY;
        }
        String upper = code.trim().toUpperCase();
        try {
            return Currency.getInstance(upper).getCurrencyCode();
        } catch (IllegalArgumentException exception) {
            throw new IllegalArgumentException("Unknown currency: " + code);
        }
    }
}
