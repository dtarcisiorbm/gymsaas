package com.gymsaas.membership.application;

import com.gymsaas.plan.domain.BillingPeriod;
import com.gymsaas.plan.domain.DurationType;
import com.gymsaas.plan.domain.FreezePolicy;
import com.gymsaas.plan.domain.Plan;
import com.gymsaas.plan.domain.PlanFeature;
import com.gymsaas.plan.domain.PlanFeatureRepository;
import com.gymsaas.plan.domain.PlanFeatureType;
import com.gymsaas.plan.domain.PlanPrice;
import com.gymsaas.plan.domain.PlanPriceRepository;
import com.gymsaas.plan.domain.PlanPriceStatus;
import com.gymsaas.plan.domain.PlanStatus;
import com.gymsaas.shared.application.ApiException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

/** The backend resolves the price; clients can only ask for a validated discount. */
@ExtendWith(MockitoExtension.class)
class MembershipPricingTest {

    private static final UUID TENANT = UUID.randomUUID();
    private static final LocalDate START = LocalDate.of(2027, 3, 10);

    @Mock private PlanPriceRepository prices;
    @Mock private PlanFeatureRepository features;

    private final Plan plan = new Plan(UUID.randomUUID(), TENANT, "MENSAL", "Mensal Livre", null,
            DurationType.MONTHLY, 1, PlanStatus.ACTIVE, new FreezePolicy(30, 2, null), "Cláusula da academia", 0,
            null, Instant.EPOCH, Instant.EPOCH);
    private final PlanPrice price = new PlanPrice(UUID.randomUUID(), TENANT, plan.id(), new BigDecimal("99.90"), "BRL",
            BillingPeriod.MONTHLY, 1, new BigDecimal("50.00"), LocalDate.of(2027, 1, 1), null, PlanPriceStatus.ACTIVE,
            null, null, Instant.EPOCH);

    @Test
    void usesPriceEffectiveOnStartDateAndSnapshotsConditions() {
        when(prices.findEffective(TENANT, plan.id(), START)).thenReturn(Optional.of(price));
        when(features.findByPlan(TENANT, plan.id())).thenReturn(List.of(new PlanFeature(UUID.randomUUID(), TENANT,
                plan.id(), PlanFeatureType.WEEKLY_CHECKIN_LIMIT, null, 3, null, null, null, null, null, 0,
                Instant.EPOCH)));

        MembershipPricing.Quote quote = pricing().quote(TENANT, plan, START, null, null, false);

        assertEquals(new BigDecimal("99.90"), quote.conditions().amount());
        assertEquals(new BigDecimal("0.00"), quote.conditions().discountAmount());
        assertEquals(new BigDecimal("99.90"), quote.conditions().finalAmount());
        assertEquals(new BigDecimal("50.00"), quote.conditions().setupFee());
        assertEquals(30, quote.conditions().freezePolicy().maxFreezeDays());
        Map<String, Object> snapshot = quote.conditions().snapshot();
        assertEquals("Mensal Livre", snapshot.get("planName"));
        assertEquals("Cláusula da academia", snapshot.get("contractTerms"));
        assertEquals(price.id().toString(), snapshot.get("priceId"));
        assertEquals(1, ((List<?>) snapshot.get("features")).size());
    }

    @Test
    void discountRequiresPermission() {
        when(prices.findEffective(TENANT, plan.id(), START)).thenReturn(Optional.of(price));
        ApiException error = assertThrows(ApiException.class,
                () -> pricing().quote(TENANT, plan, START, new BigDecimal("10.00"), "promo", false));
        assertEquals("DISCOUNT_NOT_ALLOWED", error.code());
        assertEquals(HttpStatus.FORBIDDEN, error.status());
    }

    @Test
    void discountCannotExceedPrice() {
        when(prices.findEffective(TENANT, plan.id(), START)).thenReturn(Optional.of(price));
        ApiException error = assertThrows(ApiException.class,
                () -> pricing().quote(TENANT, plan, START, new BigDecimal("99.91"), "promo", true));
        assertEquals("DISCOUNT_EXCEEDS_AMOUNT", error.code());
    }

    @Test
    void discountRequiresReason() {
        when(prices.findEffective(TENANT, plan.id(), START)).thenReturn(Optional.of(price));
        ApiException error = assertThrows(ApiException.class,
                () -> pricing().quote(TENANT, plan, START, new BigDecimal("10.00"), "  ", true));
        assertEquals("DISCOUNT_REASON_REQUIRED", error.code());
    }

    @Test
    void discountWithMoreThanTwoDecimalsIsRejected() {
        when(prices.findEffective(TENANT, plan.id(), START)).thenReturn(Optional.of(price));
        ApiException error = assertThrows(ApiException.class,
                () -> pricing().quote(TENANT, plan, START, new BigDecimal("1.005"), "promo", true));
        assertEquals("INVALID_AMOUNT", error.code());
    }

    @Test
    void validDiscountReducesFinalAmountExactly() {
        when(prices.findEffective(TENANT, plan.id(), START)).thenReturn(Optional.of(price));
        when(features.findByPlan(TENANT, plan.id())).thenReturn(List.of());
        MembershipPricing.Quote quote = pricing().quote(TENANT, plan, START, new BigDecimal("10"), "Indicação", true);
        assertEquals(new BigDecimal("10.00"), quote.conditions().discountAmount());
        assertEquals(new BigDecimal("89.90"), quote.conditions().finalAmount());
        assertEquals("Indicação", quote.conditions().discountReason());
    }

    @Test
    void planWithoutEffectivePriceCannotBeContracted() {
        when(prices.findEffective(TENANT, plan.id(), START)).thenReturn(Optional.empty());
        ApiException error = assertThrows(ApiException.class,
                () -> pricing().quote(TENANT, plan, START, null, null, false));
        assertEquals("NO_EFFECTIVE_PRICE", error.code());
    }

    private MembershipPricing pricing() {
        return new MembershipPricing(prices, features);
    }
}
