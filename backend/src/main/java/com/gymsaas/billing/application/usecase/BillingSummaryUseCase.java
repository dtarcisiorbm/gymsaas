package com.gymsaas.billing.application.usecase;

import com.gymsaas.billing.application.BillingDtos;
import com.gymsaas.billing.domain.BillingTotals;
import com.gymsaas.billing.domain.ChargeRepository;
import com.gymsaas.branch.domain.BranchRepository;
import com.gymsaas.shared.application.ApiException;
import com.gymsaas.shared.domain.Money;
import com.gymsaas.tenant.application.TenantCalendar;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.time.temporal.TemporalAdjusters;
import java.util.UUID;

/**
 * Period defaults to the current month in the tenant's zone. Receivable = open,
 * not overdue, due in the period; overdue = all open charges due before today;
 * received/refunded = payments within the period (tenant zone).
 */
@Service
public class BillingSummaryUseCase {

    private static final long MAX_DAYS = 366;

    private final ChargeRepository charges;
    private final BranchRepository branches;
    private final TenantCalendar calendar;

    public BillingSummaryUseCase(ChargeRepository charges, BranchRepository branches, TenantCalendar calendar) {
        this.charges = charges;
        this.branches = branches;
        this.calendar = calendar;
    }

    @Transactional(readOnly = true)
    public BillingDtos.SummaryView execute(UUID tenantId, LocalDate from, LocalDate to, UUID branchId) {
        LocalDate today = calendar.today(tenantId);
        LocalDate start = from != null ? from : today.withDayOfMonth(1);
        LocalDate end = to != null ? to : start.with(TemporalAdjusters.lastDayOfMonth());
        if (end.isBefore(start) || ChronoUnit.DAYS.between(start, end) >= MAX_DAYS) {
            throw ApiException.badRequest("INVALID_PERIOD", "Período inválido (máximo de 366 dias).");
        }
        if (branchId != null && branches.findById(tenantId, branchId).isEmpty()) {
            throw ApiException.notFound("BRANCH_NOT_FOUND", "Unidade não encontrada.");
        }
        ZoneId zone = calendar.zone(tenantId);
        BillingTotals totals = charges.totals(tenantId, branchId, start, end, today,
                start.atStartOfDay(zone).toInstant(), end.plusDays(1).atStartOfDay(zone).toInstant());
        return new BillingDtos.SummaryView(start, end, today, branchId, Money.DEFAULT_CURRENCY,
                view(totals.receivable()), view(totals.overdue()), view(totals.received()),
                totals.receivedByMethod().entrySet().stream()
                        .map(entry -> new BillingDtos.MethodAmountView(entry.getKey().name(),
                                entry.getValue().count(), Money.round(entry.getValue().total())))
                        .toList(),
                view(totals.refunded()));
    }

    private static BillingDtos.AmountView view(BillingTotals.Amount amount) {
        return new BillingDtos.AmountView(amount.count(), Money.round(amount.total()));
    }
}
