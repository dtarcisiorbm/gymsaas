package com.gymsaas.plan.application;

import com.gymsaas.audit.application.AuditService;
import com.gymsaas.plan.application.usecase.AddPlanPriceUseCase;
import com.gymsaas.plan.domain.BillingPeriod;
import com.gymsaas.plan.domain.DurationType;
import com.gymsaas.plan.domain.FreezePolicy;
import com.gymsaas.plan.domain.Plan;
import com.gymsaas.plan.domain.PlanPrice;
import com.gymsaas.plan.domain.PlanPriceRepository;
import com.gymsaas.plan.domain.PlanPriceStatus;
import com.gymsaas.plan.domain.PlanRepository;
import com.gymsaas.plan.domain.PlanStatus;
import com.gymsaas.shared.application.ApiException;
import com.gymsaas.tenant.application.TenantCalendar;
import com.gymsaas.tenant.domain.TenantCalendarRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** Prices are a history: never edited, never retroactive, the previous one is closed the day before. */
@ExtendWith(MockitoExtension.class)
class AddPlanPriceUseCaseTest {

    private static final UUID TENANT = UUID.randomUUID();
    private static final Clock CLOCK = Clock.fixed(Instant.parse("2027-03-10T15:00:00Z"), ZoneOffset.UTC);

    @Mock private PlanRepository plans;
    @Mock private PlanPriceRepository prices;
    @Mock private AuditService audit;
    @Mock private TenantCalendarRepository calendarRepository;

    private final Plan plan = new Plan(UUID.randomUUID(), TENANT, "MENSAL", "Mensal", null, DurationType.MONTHLY, 1,
            PlanStatus.ACTIVE, FreezePolicy.unlimited(), null, 0, null, Instant.EPOCH, Instant.EPOCH);
    private AddPlanPriceUseCase useCase;

    @BeforeEach
    void setUp() {
        lenient().when(calendarRepository.findTimezone(any())).thenReturn(Optional.of("America/Sao_Paulo"));
        PlanGuard guard = new PlanGuard(plans);
        useCase = new AddPlanPriceUseCase(guard, prices, new PlanPriceFactory(guard),
                new TenantCalendar(calendarRepository, CLOCK), audit);
        when(plans.findByIdForUpdate(TENANT, plan.id())).thenReturn(Optional.of(plan));
    }

    @Test
    void newPriceClosesCurrentPriceTheDayBefore() {
        PlanPrice current = price(new BigDecimal("99.90"), LocalDate.of(2027, 1, 1), null);
        when(prices.findByPlan(TENANT, plan.id())).thenReturn(List.of(current));
        when(prices.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        PlanDtos.PriceView view = useCase.execute(TENANT, plan.id(), request("119.90", "MONTHLY",
                LocalDate.of(2027, 4, 1)));

        ArgumentCaptor<PlanPrice> saved = ArgumentCaptor.forClass(PlanPrice.class);
        InOrder order = inOrder(prices);
        order.verify(prices, times(2)).save(saved.capture());
        PlanPrice closed = saved.getAllValues().get(0);
        PlanPrice created = saved.getAllValues().get(1);
        assertEquals(current.id(), closed.id());
        assertEquals(LocalDate.of(2027, 3, 31), closed.validUntil());
        assertEquals(new BigDecimal("99.90"), closed.amount());
        assertEquals(new BigDecimal("119.90"), created.amount());
        assertNull(created.validUntil());
        assertTrue(view.scheduled());
    }

    @Test
    void retroactivePriceIsRejected() {
        ApiException error = assertThrows(ApiException.class, () -> useCase.execute(TENANT, plan.id(),
                request("119.90", "MONTHLY", LocalDate.of(2027, 3, 9))));
        assertEquals("PRICE_RETROACTIVE", error.code());
        verify(prices, never()).save(any());
    }

    @Test
    void scheduledPriceMustBeCancelledFirst() {
        when(prices.findByPlan(TENANT, plan.id())).thenReturn(List.of(
                price(new BigDecimal("129.90"), LocalDate.of(2027, 5, 1), null),
                price(new BigDecimal("99.90"), LocalDate.of(2027, 1, 1), LocalDate.of(2027, 4, 30))));
        ApiException error = assertThrows(ApiException.class, () -> useCase.execute(TENANT, plan.id(),
                request("119.90", "MONTHLY", LocalDate.of(2027, 4, 1))));
        assertEquals("SCHEDULED_PRICE_EXISTS", error.code());
    }

    @Test
    void billingLongerThanContractIsRejected() {
        when(prices.findByPlan(TENANT, plan.id())).thenReturn(List.of());
        ApiException error = assertThrows(ApiException.class, () -> useCase.execute(TENANT, plan.id(),
                request("1199.00", "ANNUAL", null)));
        assertEquals("BILLING_PERIOD_INCOMPATIBLE", error.code());
    }

    @Test
    void amountWithThreeDecimalsIsRejected() {
        when(prices.findByPlan(TENANT, plan.id())).thenReturn(List.of());
        ApiException error = assertThrows(ApiException.class, () -> useCase.execute(TENANT, plan.id(),
                request("99.999", "MONTHLY", null)));
        assertEquals("INVALID_AMOUNT", error.code());
    }

    private PlanPrice price(BigDecimal amount, LocalDate from, LocalDate until) {
        return new PlanPrice(UUID.randomUUID(), TENANT, plan.id(), amount, "BRL", BillingPeriod.MONTHLY, 1,
                new BigDecimal("0.00"), from, until, PlanPriceStatus.ACTIVE, null, null, Instant.EPOCH);
    }

    private static PlanDtos.PriceRequest request(String amount, String period, LocalDate validFrom) {
        return new PlanDtos.PriceRequest(new BigDecimal(amount), "BRL", period, 1, null, validFrom, null);
    }
}
