package com.gymsaas.billing.application;

import com.gymsaas.billing.domain.BillingNumberGenerator;
import com.gymsaas.billing.domain.BillingSchedule;
import com.gymsaas.billing.domain.Charge;
import com.gymsaas.billing.domain.ChargeKind;
import com.gymsaas.billing.domain.ChargeRepository;
import com.gymsaas.billing.domain.ChargeStatus;
import com.gymsaas.membership.application.MembershipBillingPort;
import com.gymsaas.membership.domain.ContractedConditions;
import com.gymsaas.membership.domain.Membership;
import com.gymsaas.shared.domain.UuidV7;
import com.gymsaas.shared.infrastructure.tenant.TenantContext;
import com.gymsaas.tenant.application.TenantCalendar;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Decisions F2/F3/F7: installments from the membership snapshot (value per
 * cycle = finalAmount), setup fee only for a new membership, and automatic
 * cancellation of future open charges when the membership is cancelled.
 * Freezes never change charges.
 */
@Component
public class MembershipBillingAdapter implements MembershipBillingPort {

    private final ChargeRepository charges;
    private final BillingNumberGenerator numbers;
    private final BillingJournal journal;
    private final TenantCalendar calendar;

    public MembershipBillingAdapter(ChargeRepository charges, BillingNumberGenerator numbers, BillingJournal journal,
                                    TenantCalendar calendar) {
        this.charges = charges;
        this.numbers = numbers;
        this.journal = journal;
        this.calendar = calendar;
    }

    @Override
    public void onContracted(Membership membership, boolean renewal) {
        UUID tenantId = membership.tenantId();
        ContractedConditions conditions = membership.conditions();
        int year = calendar.today(tenantId).getYear();
        Instant now = calendar.now();
        UUID actor = TenantContext.userId();
        String label = label(membership);
        List<Charge> created = new ArrayList<>();

        BigDecimal setupFee = conditions.setupFee();
        if (!renewal && setupFee != null && setupFee.signum() > 0) {
            created.add(charges.insert(new Charge(UuidV7.generate(), tenantId, numbers.nextCharge(tenantId, year),
                    membership.studentId(), membership.id(), membership.branchId(), ChargeKind.SETUP_FEE, null, null,
                    "Taxa de adesão — " + label, setupFee, conditions.currency(), membership.startDate(), null, null,
                    ChargeStatus.OPEN, null, null, null, null, null, actor, now, now)));
        }
        if (conditions.finalAmount().signum() > 0) {
            for (BillingSchedule.Installment installment : BillingSchedule.installments(membership.startDate(),
                    membership.originalEndDate(), conditions.durationType(), conditions.durationValue(),
                    conditions.billingPeriod(), conditions.billingInterval())) {
                created.add(charges.insert(new Charge(UuidV7.generate(), tenantId,
                        numbers.nextCharge(tenantId, year), membership.studentId(), membership.id(),
                        membership.branchId(), ChargeKind.INSTALLMENT, installment.number(), installment.count(),
                        "Parcela %d/%d — %s".formatted(installment.number(), installment.count(), label),
                        conditions.finalAmount(), conditions.currency(), installment.dueDate(),
                        installment.periodStart(), installment.periodEnd(), ChargeStatus.OPEN, null, null, null, null,
                        null, actor, now, now)));
            }
        }
        if (!created.isEmpty()) {
            BigDecimal total = created.stream().map(Charge::amount).reduce(BigDecimal.ZERO, BigDecimal::add);
            journal.record(tenantId, "BILLING_CHARGES_GENERATED", "membership", membership.id(), "MEDIUM", null, null,
                    Map.of("registrationNumber", membership.registrationNumber(),
                            "renewal", renewal,
                            "count", created.size(),
                            "total", total.toPlainString(),
                            "chargeNumbers", created.stream().map(Charge::chargeNumber).toList()));
        }
    }

    @Override
    public void onCancelled(Membership membership, LocalDate effectiveDate, String reason) {
        UUID tenantId = membership.tenantId();
        Instant now = calendar.now();
        UUID actor = TenantContext.userId();
        String chargeReason = BillingJournal.limit("Matrícula " + membership.registrationNumber() + " cancelada: "
                + reason, 1000);
        List<String> cancelled = new ArrayList<>();
        for (Charge charge : charges.lockOpenByMembershipDueFrom(tenantId, membership.id(), effectiveDate)) {
            cancelled.add(charges.updateStatus(charge.cancelled(now, actor, chargeReason)).chargeNumber());
        }
        if (!cancelled.isEmpty()) {
            journal.record(tenantId, "BILLING_CHARGES_CANCELLED", "membership", membership.id(), "HIGH", null, null,
                    Map.of("registrationNumber", membership.registrationNumber(),
                            "effectiveDate", effectiveDate.toString(),
                            "count", cancelled.size(),
                            "chargeNumbers", cancelled));
        }
    }

    private static String label(Membership membership) {
        Object planName = membership.conditions().snapshot().get("planName");
        String plan = planName == null ? "Plano" : planName.toString();
        return BillingJournal.limit(plan, 240) + " (" + membership.registrationNumber() + ")";
    }
}
