package com.gymsaas.plan.application;

import com.gymsaas.plan.domain.BillingPeriod;
import com.gymsaas.plan.domain.Plan;
import com.gymsaas.plan.domain.PlanPrice;
import com.gymsaas.plan.domain.PlanPriceStatus;
import com.gymsaas.shared.application.ApiException;
import com.gymsaas.shared.domain.Money;
import com.gymsaas.shared.domain.UuidV7;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

/** Validates a price request against the plan and builds an immutable price row. */
@Component
public class PlanPriceFactory {

    private final PlanGuard guard;

    public PlanPriceFactory(PlanGuard guard) {
        this.guard = guard;
    }

    public PlanPrice build(Plan plan, PlanDtos.PriceRequest request, LocalDate validFrom, UUID actor, Instant now) {
        BigDecimal amount = PlanGuard.money(request.amount(), "amount");
        BigDecimal setupFee = request.setupFee() == null ? Money.zero() : PlanGuard.money(request.setupFee(), "setupFee");
        String currency = PlanGuard.currency(request.currency());
        BillingPeriod period = guard.billingPeriod(request.billingPeriod());
        int interval = request.billingInterval() == null ? 1 : request.billingInterval();
        if (!period.compatibleWith(plan.durationType(), plan.durationValue(), interval)) {
            throw ApiException.badRequest("BILLING_PERIOD_INCOMPATIBLE",
                    "A periodicidade de cobrança não pode exceder a duração do contrato "
                            + "(planos em dias aceitam apenas cobrança única).");
        }
        return new PlanPrice(UuidV7.generate(), plan.tenantId(), plan.id(), amount, currency, period, interval,
                setupFee, validFrom, null, PlanPriceStatus.ACTIVE, PlanGuard.trimToNull(request.notes()),
                actor, now);
    }
}
