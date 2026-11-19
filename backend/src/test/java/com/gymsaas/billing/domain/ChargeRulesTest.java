package com.gymsaas.billing.domain;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ChargeRulesTest {

    private static final Instant NOW = Instant.parse("2026-11-19T12:00:00Z");
    private static final LocalDate DUE = LocalDate.of(2026, 11, 10);

    private static Charge open() {
        return new Charge(UUID.randomUUID(), UUID.randomUUID(), "C2026000001", UUID.randomUUID(), null,
                UUID.randomUUID(), ChargeKind.MANUAL, null, null, "Diária", new BigDecimal("30.00"), "BRL", DUE, null,
                null, ChargeStatus.OPEN, null, null, null, null, null, null, NOW, NOW);
    }

    private static Payment confirmed(Charge charge) {
        return new Payment(UUID.randomUUID(), charge.tenantId(), "R2026000001", charge.id(), charge.studentId(),
                charge.branchId(), charge.amount(), "BRL", PaymentMethod.PIX, NOW, null, null,
                PaymentStatus.CONFIRMED, null, null, null, null, NOW);
    }

    @Test
    void overdueIsDerivedFromOpenStatusAndDueDate() {
        Charge charge = open();
        assertTrue(charge.overdue(DUE.plusDays(1)));
        assertFalse(charge.overdue(DUE));
        assertFalse(charge.paid(NOW).overdue(DUE.plusDays(30)));
    }

    @Test
    void openToPaidToOpenOnRefund() {
        Charge paid = open().paid(NOW);
        assertEquals(ChargeStatus.PAID, paid.status());
        assertEquals(NOW, paid.paidAt());
        Charge reopened = paid.reopened(NOW.plusSeconds(60));
        assertEquals(ChargeStatus.OPEN, reopened.status());
        assertNull(reopened.paidAt());
        assertEquals(paid.amount(), reopened.amount());
    }

    @Test
    void invalidTransitionsAreRejected() {
        Charge paid = open().paid(NOW);
        assertThrows(IllegalStateException.class, () -> paid.paid(NOW));
        assertThrows(IllegalStateException.class, () -> paid.cancelled(NOW, null, "x"));
        Charge cancelled = open().cancelled(NOW, null, "Lançada por engano");
        assertThrows(IllegalStateException.class, () -> cancelled.paid(NOW));
        assertThrows(IllegalStateException.class, () -> cancelled.reopened(NOW));
        assertThrows(IllegalStateException.class, () -> open().reopened(NOW));
        assertThrows(IllegalArgumentException.class, () -> open().cancelled(NOW, null, " "));
    }

    @Test
    void chargeRequiresPositiveAmountAndMembershipForGeneratedKinds() {
        assertThrows(IllegalArgumentException.class, () -> new Charge(UUID.randomUUID(), UUID.randomUUID(), "C",
                UUID.randomUUID(), null, UUID.randomUUID(), ChargeKind.MANUAL, null, null, "x", BigDecimal.ZERO,
                "BRL", DUE, null, null, ChargeStatus.OPEN, null, null, null, null, null, null, NOW, NOW));
        assertThrows(IllegalArgumentException.class, () -> new Charge(UUID.randomUUID(), UUID.randomUUID(), "C",
                UUID.randomUUID(), null, UUID.randomUUID(), ChargeKind.INSTALLMENT, 1, 1, "x", BigDecimal.TEN,
                "BRL", DUE, null, null, ChargeStatus.OPEN, null, null, null, null, null, null, NOW, NOW));
    }

    @Test
    void paymentSettlesOnlyTheExactAmount() {
        Charge charge = open();
        assertTrue(Payment.settles(charge, new BigDecimal("30")));
        assertTrue(Payment.settles(charge, new BigDecimal("30.00")));
        assertFalse(Payment.settles(charge, new BigDecimal("29.99")));
        assertFalse(Payment.settles(charge, new BigDecimal("30.01")));
        assertFalse(Payment.settles(charge, null));
    }

    @Test
    void paymentRefundOnlyOnceWithReason() {
        Payment payment = confirmed(open());
        Payment refunded = payment.refunded(NOW, null, " Duplicado ");
        assertEquals(PaymentStatus.REFUNDED, refunded.status());
        assertEquals("Duplicado", refunded.refundReason());
        assertThrows(IllegalStateException.class, () -> refunded.refunded(NOW, null, "x"));
        assertThrows(IllegalArgumentException.class, () -> payment.refunded(NOW, null, ""));
    }

    @Test
    void numbersArePrefixedPerKind() {
        assertEquals("C2026000042", BillingNumberGenerator.format('C', 2026, 42));
        assertEquals("R2026999999", BillingNumberGenerator.format('R', 2026, 999_999));
        assertThrows(IllegalArgumentException.class, () -> BillingNumberGenerator.format('C', 2026, 0));
    }
}
