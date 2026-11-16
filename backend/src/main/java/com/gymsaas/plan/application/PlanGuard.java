package com.gymsaas.plan.application;

import com.gymsaas.plan.domain.BillingPeriod;
import com.gymsaas.plan.domain.DurationType;
import com.gymsaas.plan.domain.FreezePolicy;
import com.gymsaas.plan.domain.Plan;
import com.gymsaas.plan.domain.PlanRepository;
import com.gymsaas.plan.domain.PlanStatus;
import com.gymsaas.shared.application.ApiException;
import com.gymsaas.shared.domain.Money;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.UUID;

/** Tenant scoping, parsing and validation shared by plan use cases. */
@Component
public class PlanGuard {

    private final PlanRepository plans;

    public PlanGuard(PlanRepository plans) {
        this.plans = plans;
    }

    public Plan requirePlan(UUID tenantId, UUID planId) {
        return plans.findById(tenantId, planId)
                .orElseThrow(() -> ApiException.notFound("PLAN_NOT_FOUND", "Plano não encontrado."));
    }

    public Plan requirePlanForUpdate(UUID tenantId, UUID planId) {
        return plans.findByIdForUpdate(tenantId, planId)
                .orElseThrow(() -> ApiException.notFound("PLAN_NOT_FOUND", "Plano não encontrado."));
    }

    public DurationType duration(String rawType, Integer rawValue) {
        DurationType type;
        try {
            type = DurationType.parse(rawType);
        } catch (IllegalArgumentException exception) {
            throw ApiException.badRequest("INVALID_DURATION", "Tipo de duração inválido.");
        }
        int value = rawValue == null ? 1 : rawValue;
        if (!type.acceptsValue(value)) {
            throw ApiException.badRequest("INVALID_DURATION", type.custom()
                    ? "Duração personalizada fora do intervalo permitido."
                    : "Durações fixas (mensal, trimestral, semestral, anual) usam valor 1.");
        }
        return type;
    }

    public static int durationValue(Integer rawValue) {
        return rawValue == null ? 1 : rawValue;
    }

    public FreezePolicy freezePolicy(PlanDtos.FreezePolicyRequest request) {
        FreezePolicy policy = request == null ? FreezePolicy.unlimited()
                : new FreezePolicy(request.maxFreezeDays(), request.maxFreezes(), request.minFreezeDays());
        String violation = policy.configurationViolation();
        if (violation != null) {
            throw ApiException.badRequest(violation,
                    "Política de congelamento inválida: o mínimo não pode exceder o máximo de dias.");
        }
        return policy;
    }

    public PlanStatus status(String raw) {
        try {
            return PlanStatus.parse(raw);
        } catch (IllegalArgumentException exception) {
            throw ApiException.badRequest("INVALID_STATUS", "Status inválido.");
        }
    }

    public BillingPeriod billingPeriod(String raw) {
        try {
            return BillingPeriod.parse(raw);
        } catch (IllegalArgumentException exception) {
            throw ApiException.badRequest("INVALID_BILLING_PERIOD", "Periodicidade de cobrança inválida.");
        }
    }

    public static BigDecimal money(BigDecimal value, String field) {
        try {
            return Money.normalize(value);
        } catch (IllegalArgumentException | NullPointerException exception) {
            throw ApiException.badRequest("INVALID_AMOUNT", "Valor inválido em " + field + ".");
        }
    }

    public static String currency(String raw) {
        try {
            String currency = Money.currency(raw);
            if (!Money.DEFAULT_CURRENCY.equals(currency)) {
                throw ApiException.badRequest("UNSUPPORTED_CURRENCY", "Somente BRL é suportado nesta fase.");
            }
            return currency;
        } catch (IllegalArgumentException exception) {
            throw ApiException.badRequest("INVALID_CURRENCY", "Moeda inválida.");
        }
    }

    public static String trimToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
