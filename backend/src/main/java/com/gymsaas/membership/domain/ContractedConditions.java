package com.gymsaas.membership.domain;

import com.gymsaas.plan.domain.BillingPeriod;
import com.gymsaas.plan.domain.DurationType;
import com.gymsaas.plan.domain.FreezePolicy;

import java.math.BigDecimal;
import java.util.Map;
import java.util.Objects;

/**
 * Commercial conditions frozen at contracting time. Values are per billing
 * cycle; setupFee is charged once. finalAmount = amount - discountAmount.
 * The snapshot keeps plan name/code, price id/validity and benefits as they
 * were when the membership was contracted.
 */
public record ContractedConditions(
        DurationType durationType,
        int durationValue,
        BigDecimal amount,
        BigDecimal setupFee,
        String currency,
        BillingPeriod billingPeriod,
        int billingInterval,
        BigDecimal discountAmount,
        String discountReason,
        BigDecimal finalAmount,
        FreezePolicy freezePolicy,
        Map<String, Object> snapshot) {

    public ContractedConditions {
        Objects.requireNonNull(amount, "amount");
        Objects.requireNonNull(discountAmount, "discountAmount");
        Objects.requireNonNull(finalAmount, "finalAmount");
        if (discountAmount.signum() < 0 || discountAmount.compareTo(amount) > 0) {
            throw new IllegalArgumentException("Discount must be between zero and the contracted amount.");
        }
        if (finalAmount.compareTo(amount.subtract(discountAmount)) != 0) {
            throw new IllegalArgumentException("Final amount must equal amount minus discount.");
        }
        if (discountAmount.signum() > 0 && (discountReason == null || discountReason.isBlank())) {
            throw new IllegalArgumentException("Discount requires a reason.");
        }
        snapshot = snapshot == null ? Map.of() : Map.copyOf(snapshot);
    }
}
