package com.gymsaas.billing.application.usecase;

import com.gymsaas.billing.application.BillingDtos;
import com.gymsaas.billing.application.BillingJournal;
import com.gymsaas.billing.application.BillingLoader;
import com.gymsaas.billing.domain.Charge;
import com.gymsaas.billing.domain.ChargeRepository;
import com.gymsaas.billing.domain.ChargeStatus;
import com.gymsaas.billing.domain.Payment;
import com.gymsaas.billing.domain.PaymentRepository;
import com.gymsaas.billing.domain.PaymentStatus;
import com.gymsaas.shared.application.ApiException;
import com.gymsaas.shared.infrastructure.tenant.TenantContext;
import com.gymsaas.tenant.application.TenantCalendar;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

/** Decision F6: payment becomes REFUNDED (reason required) and the charge is OPEN again. Nothing is deleted. */
@Service
public class RefundPaymentUseCase {

    private final ChargeRepository charges;
    private final PaymentRepository payments;
    private final BillingJournal journal;
    private final BillingLoader loader;
    private final TenantCalendar calendar;

    public RefundPaymentUseCase(ChargeRepository charges, PaymentRepository payments, BillingJournal journal,
                                BillingLoader loader, TenantCalendar calendar) {
        this.charges = charges;
        this.payments = payments;
        this.journal = journal;
        this.loader = loader;
        this.calendar = calendar;
    }

    @Transactional
    public BillingDtos.ChargeDetail execute(UUID tenantId, UUID paymentId, BillingDtos.ReasonRequest request) {
        Payment found = payments.findById(tenantId, paymentId)
                .orElseThrow(() -> ApiException.notFound("PAYMENT_NOT_FOUND", "Recebimento não encontrado."));
        // Lock the charge first (same order as RegisterPayment), then re-read the payment under the lock.
        Charge charge = charges.lockById(tenantId, found.chargeId())
                .orElseThrow(() -> ApiException.notFound("CHARGE_NOT_FOUND", "Cobrança não encontrada."));
        Payment payment = payments.findById(tenantId, paymentId).orElseThrow();
        if (payment.status() != PaymentStatus.CONFIRMED || charge.status() != ChargeStatus.PAID) {
            throw ApiException.conflict("PAYMENT_NOT_REFUNDABLE", "Este recebimento já foi estornado.");
        }
        Instant now = calendar.now();
        String reason = request.reason().trim();
        Payment refunded = payments.markRefunded(payment.refunded(now, TenantContext.userId(), reason));
        Charge saved = charges.updateStatus(charge.reopened(now));
        journal.record(tenantId, "PAYMENT_REFUNDED", "charge", saved.id(), "HIGH", charge.status().name(),
                saved.status().name(), Map.of("chargeNumber", saved.chargeNumber(),
                        "paymentId", refunded.id().toString(),
                        "paymentNumber", refunded.paymentNumber(),
                        "amount", refunded.amount().toPlainString(),
                        "reason", reason));
        return loader.detail(tenantId, saved);
    }
}
