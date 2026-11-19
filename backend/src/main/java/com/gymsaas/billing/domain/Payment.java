package com.gymsaas.billing.domain;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/** A manual receipt that settles one charge in full (decision F5). */
public record Payment(
        UUID id,
        UUID tenantId,
        String paymentNumber,
        UUID chargeId,
        UUID studentId,
        UUID branchId,
        BigDecimal amount,
        String currency,
        PaymentMethod method,
        Instant paidAt,
        String reference,
        String notes,
        PaymentStatus status,
        UUID receivedBy,
        Instant refundedAt,
        UUID refundedBy,
        String refundReason,
        Instant createdAt) {

    public Payment {
        Objects.requireNonNull(amount, "amount");
        Objects.requireNonNull(method, "method");
        Objects.requireNonNull(paidAt, "paidAt");
        Objects.requireNonNull(status, "status");
        if (amount.signum() <= 0) {
            throw new IllegalArgumentException("Payment amount must be positive.");
        }
    }

    /** Settles {@code charge} only when the amount and currency match exactly. */
    public static boolean settles(Charge charge, BigDecimal amount) {
        return amount != null && charge.amount().compareTo(amount) == 0;
    }

    public Payment refunded(Instant at, UUID by, String reason) {
        if (status != PaymentStatus.CONFIRMED) {
            throw new IllegalStateException("Only confirmed payments can be refunded.");
        }
        if (reason == null || reason.isBlank()) {
            throw new IllegalArgumentException("Refund requires a reason.");
        }
        return new Payment(id, tenantId, paymentNumber, chargeId, studentId, branchId, amount, currency, method, paidAt,
                reference, notes, PaymentStatus.REFUNDED, receivedBy, at, by, reason.trim(), createdAt);
    }
}
