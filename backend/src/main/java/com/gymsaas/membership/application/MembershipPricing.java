package com.gymsaas.membership.application;

import com.gymsaas.membership.domain.ContractedConditions;
import com.gymsaas.plan.domain.Plan;
import com.gymsaas.plan.domain.PlanFeature;
import com.gymsaas.plan.domain.PlanFeatureRepository;
import com.gymsaas.plan.domain.PlanPrice;
import com.gymsaas.plan.domain.PlanPriceRepository;
import com.gymsaas.shared.application.ApiException;
import com.gymsaas.shared.domain.Money;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Resolves the price effective on the membership start date and builds the
 * immutable contracted conditions. The client never sends amounts; it may
 * only request a discount, which requires a specific permission, cannot
 * exceed the price and needs a reason.
 */
@Component
public class MembershipPricing {

    public record Quote(PlanPrice price, ContractedConditions conditions) {
    }

    private final PlanPriceRepository prices;
    private final PlanFeatureRepository features;

    public MembershipPricing(PlanPriceRepository prices, PlanFeatureRepository features) {
        this.prices = prices;
        this.features = features;
    }

    public Quote quote(UUID tenantId, Plan plan, LocalDate startDate, BigDecimal requestedDiscount,
                       String requestedReason, boolean canApplyDiscount) {
        PlanPrice price = prices.findEffective(tenantId, plan.id(), startDate)
                .orElseThrow(() -> ApiException.conflict("NO_EFFECTIVE_PRICE",
                        "O plano não possui preço vigente na data de início."));

        BigDecimal discount;
        try {
            discount = Money.normalizeOrZero(requestedDiscount);
        } catch (IllegalArgumentException exception) {
            throw ApiException.badRequest("INVALID_AMOUNT", "Desconto inválido.");
        }
        String reason = MembershipGuard.trimToNull(requestedReason);
        if (discount.signum() > 0) {
            if (!canApplyDiscount) {
                throw ApiException.forbidden("DISCOUNT_NOT_ALLOWED", "Sem permissão para conceder desconto.");
            }
            if (discount.compareTo(price.amount()) > 0) {
                throw ApiException.badRequest("DISCOUNT_EXCEEDS_AMOUNT",
                        "O desconto não pode exceder o valor do plano.");
            }
            if (reason == null) {
                throw ApiException.badRequest("DISCOUNT_REASON_REQUIRED", "Informe o motivo do desconto.");
            }
        } else {
            reason = null;
        }

        BigDecimal finalAmount = price.amount().subtract(discount);
        ContractedConditions conditions = new ContractedConditions(plan.durationType(), plan.durationValue(),
                price.amount(), price.setupFee(), price.currency(), price.billingPeriod(), price.billingInterval(),
                discount, reason, finalAmount, plan.freezePolicy(),
                snapshot(plan, price, features.findByPlan(tenantId, plan.id())));
        return new Quote(price, conditions);
    }

    /** JSON-friendly snapshot (strings/numbers/lists/maps only). */
    private static Map<String, Object> snapshot(Plan plan, PlanPrice price, List<PlanFeature> planFeatures) {
        Map<String, Object> snapshot = new LinkedHashMap<>();
        snapshot.put("planId", plan.id().toString());
        snapshot.put("planCode", plan.code());
        snapshot.put("planName", plan.name());
        if (plan.description() != null) {
            snapshot.put("planDescription", plan.description());
        }
        if (plan.contractTerms() != null) {
            snapshot.put("contractTerms", plan.contractTerms());
        }
        snapshot.put("priceId", price.id().toString());
        snapshot.put("priceValidFrom", price.validFrom().toString());
        snapshot.put("amount", price.amount().toPlainString());
        snapshot.put("setupFee", price.setupFee().toPlainString());
        snapshot.put("currency", price.currency());
        snapshot.put("billingPeriod", price.billingPeriod().name());
        snapshot.put("billingInterval", price.billingInterval());
        snapshot.put("features", planFeatures.stream().map(MembershipPricing::feature).toList());
        return snapshot;
    }

    private static Map<String, Object> feature(PlanFeature feature) {
        Map<String, Object> value = new HashMap<>();
        value.put("type", feature.type().name());
        if (feature.branchId() != null) {
            value.put("branchId", feature.branchId().toString());
        }
        if (feature.intValue() != null) {
            value.put("intValue", feature.intValue());
        }
        if (feature.startTime() != null) {
            value.put("startTime", feature.startTime().toString());
        }
        if (feature.endTime() != null) {
            value.put("endTime", feature.endTime().toString());
        }
        if (feature.daysOfWeek() != null) {
            value.put("daysOfWeek", feature.daysOfWeek());
        }
        if (feature.label() != null) {
            value.put("label", feature.label());
        }
        if (feature.description() != null) {
            value.put("description", feature.description());
        }
        return value;
    }
}
