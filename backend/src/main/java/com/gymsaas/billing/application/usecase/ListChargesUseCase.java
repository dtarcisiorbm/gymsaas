package com.gymsaas.billing.application.usecase;

import com.gymsaas.billing.application.BillingDtos;
import com.gymsaas.billing.application.BillingJournal;
import com.gymsaas.billing.application.BillingLoader;
import com.gymsaas.billing.domain.Charge;
import com.gymsaas.billing.domain.ChargeRepository;
import com.gymsaas.billing.domain.ChargeSearchCriteria;
import com.gymsaas.billing.domain.ChargeStatus;
import com.gymsaas.shared.application.ApiException;
import com.gymsaas.shared.domain.PageResult;
import com.gymsaas.tenant.application.TenantCalendar;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/** Status filter accepts OPEN, PAID, CANCELLED and the derived OVERDUE (decision F4). */
@Service
public class ListChargesUseCase {

    private final ChargeRepository charges;
    private final BillingLoader loader;
    private final TenantCalendar calendar;

    public ListChargesUseCase(ChargeRepository charges, BillingLoader loader, TenantCalendar calendar) {
        this.charges = charges;
        this.loader = loader;
        this.calendar = calendar;
    }

    @Transactional(readOnly = true)
    public PageResult<BillingDtos.ChargeView> execute(UUID tenantId, BillingDtos.ChargeFilter filter) {
        int page = Math.max(0, filter.page());
        int size = filter.size() <= 0 ? 20 : Math.min(filter.size(), 100);
        if (filter.dueFrom() != null && filter.dueTo() != null && filter.dueTo().isBefore(filter.dueFrom())) {
            throw ApiException.badRequest("INVALID_PERIOD", "Período de vencimento inválido.");
        }
        String rawStatus = BillingJournal.trimToNull(filter.status());
        boolean overdueOnly = "OVERDUE".equalsIgnoreCase(rawStatus);
        ChargeStatus status = null;
        if (rawStatus != null && !overdueOnly) {
            try {
                status = ChargeStatus.parse(rawStatus);
            } catch (IllegalArgumentException exception) {
                throw ApiException.badRequest("INVALID_STATUS", "Status inválido.");
            }
        }
        PageResult<Charge> result = charges.search(tenantId, new ChargeSearchCriteria(
                BillingJournal.trimToNull(filter.search()), status, overdueOnly, calendar.today(tenantId),
                filter.studentId(), filter.membershipId(), filter.branchId(), filter.dueFrom(), filter.dueTo(), page,
                size, BillingJournal.trimToNull(filter.sort()), filter.direction()));
        return PageResult.of(loader.views(tenantId, result.content()), result.totalElements(), page, size);
    }
}
