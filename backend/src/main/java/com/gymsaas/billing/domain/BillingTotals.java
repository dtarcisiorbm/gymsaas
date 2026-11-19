package com.gymsaas.billing.domain;

import java.math.BigDecimal;
import java.util.Map;

/**
 * Aggregates for the billing summary.
 *
 * @param receivable   OPEN, not overdue, due within the period
 * @param overdue      OPEN and due before today (all time: outstanding debt)
 * @param received     CONFIRMED payments received within the period, by method
 * @param refunded     payments refunded within the period
 */
public record BillingTotals(
        Amount receivable,
        Amount overdue,
        Amount received,
        Map<PaymentMethod, Amount> receivedByMethod,
        Amount refunded) {

    public record Amount(long count, BigDecimal total) {
    }
}
