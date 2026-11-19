package com.gymsaas.billing.application.usecase;

import com.gymsaas.billing.application.BillingDtos;
import com.gymsaas.billing.application.BillingJournal;
import com.gymsaas.billing.application.BillingLoader;
import com.gymsaas.billing.domain.Charge;
import com.gymsaas.billing.domain.ChargeRepository;
import com.gymsaas.billing.domain.ChargeStatus;
import com.gymsaas.shared.application.ApiException;
import com.gymsaas.shared.infrastructure.tenant.TenantContext;
import com.gymsaas.tenant.application.TenantCalendar;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;
import java.util.UUID;

/** Decision F7: only OPEN charges, mandatory reason; the row is kept. */
@Service
public class CancelChargeUseCase {

    private final ChargeRepository charges;
    private final BillingJournal journal;
    private final BillingLoader loader;
    private final TenantCalendar calendar;

    public CancelChargeUseCase(ChargeRepository charges, BillingJournal journal, BillingLoader loader,
                               TenantCalendar calendar) {
        this.charges = charges;
        this.journal = journal;
        this.loader = loader;
        this.calendar = calendar;
    }

    @Transactional
    public BillingDtos.ChargeDetail execute(UUID tenantId, UUID chargeId, BillingDtos.ReasonRequest request) {
        Charge current = charges.lockById(tenantId, chargeId)
                .orElseThrow(() -> ApiException.notFound("CHARGE_NOT_FOUND", "Cobrança não encontrada."));
        if (current.status() != ChargeStatus.OPEN) {
            throw ApiException.conflict("CHARGE_NOT_OPEN", "Somente cobranças em aberto podem ser canceladas.");
        }
        String reason = request.reason().trim();
        Charge saved = charges.updateStatus(current.cancelled(calendar.now(), TenantContext.userId(), reason));
        journal.record(tenantId, "CHARGE_CANCELLED", "charge", saved.id(), "MEDIUM", current.status().name(),
                saved.status().name(), Map.of("chargeNumber", saved.chargeNumber(), "reason", reason,
                        "amount", saved.amount().toPlainString()));
        return loader.detail(tenantId, saved);
    }
}
