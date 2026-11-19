package com.gymsaas.billing.application.usecase;

import com.gymsaas.billing.application.BillingDtos;
import com.gymsaas.billing.application.BillingJournal;
import com.gymsaas.billing.application.BillingLoader;
import com.gymsaas.billing.domain.BillingNumberGenerator;
import com.gymsaas.billing.domain.Charge;
import com.gymsaas.billing.domain.ChargeRepository;
import com.gymsaas.billing.domain.ChargeStatus;
import com.gymsaas.billing.domain.Payment;
import com.gymsaas.billing.domain.PaymentMethod;
import com.gymsaas.billing.domain.PaymentRepository;
import com.gymsaas.billing.domain.PaymentStatus;
import com.gymsaas.shared.application.ApiException;
import com.gymsaas.shared.domain.Money;
import com.gymsaas.shared.domain.UuidV7;
import com.gymsaas.shared.infrastructure.tenant.TenantContext;
import com.gymsaas.tenant.application.TenantCalendar;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;

/**
 * Manual receipt (decisions F1/F5). The charge row is locked, so concurrent
 * attempts are serialized: the second sees the charge PAID and gets 409. The
 * partial unique index on payments is the last line of defense.
 */
@Service
public class RegisterPaymentUseCase {

    /** Tolerance for clock skew between the operator's device and the server. */
    private static final Duration FUTURE_TOLERANCE = Duration.ofMinutes(5);

    private final ChargeRepository charges;
    private final PaymentRepository payments;
    private final BillingNumberGenerator numbers;
    private final BillingJournal journal;
    private final BillingLoader loader;
    private final TenantCalendar calendar;

    public RegisterPaymentUseCase(ChargeRepository charges, PaymentRepository payments,
                                  BillingNumberGenerator numbers, BillingJournal journal, BillingLoader loader,
                                  TenantCalendar calendar) {
        this.charges = charges;
        this.payments = payments;
        this.numbers = numbers;
        this.journal = journal;
        this.loader = loader;
        this.calendar = calendar;
    }

    @Transactional
    public BillingDtos.ChargeDetail execute(UUID tenantId, UUID chargeId, BillingDtos.RegisterPaymentRequest request) {
        PaymentMethod method;
        try {
            method = PaymentMethod.parse(request.method());
        } catch (IllegalArgumentException exception) {
            throw ApiException.badRequest("INVALID_METHOD", "Forma de pagamento inválida.");
        }
        Instant now = calendar.now();
        Instant paidAt = request.paidAt() == null ? now : request.paidAt();
        if (paidAt.isAfter(now.plus(FUTURE_TOLERANCE))) {
            throw ApiException.badRequest("PAID_AT_IN_FUTURE", "A data do recebimento não pode estar no futuro.");
        }
        BigDecimal amount;
        try {
            amount = Money.normalize(request.amount());
        } catch (IllegalArgumentException exception) {
            throw ApiException.badRequest("INVALID_AMOUNT", "Valor inválido.");
        }

        Charge charge = charges.lockById(tenantId, chargeId)
                .orElseThrow(() -> ApiException.notFound("CHARGE_NOT_FOUND", "Cobrança não encontrada."));
        if (charge.status() != ChargeStatus.OPEN) {
            throw ApiException.conflict("CHARGE_NOT_OPEN", charge.status() == ChargeStatus.PAID
                    ? "Esta cobrança já está quitada." : "Cobrança cancelada não pode receber pagamento.");
        }
        if (!Payment.settles(charge, amount)) {
            throw ApiException.badRequest("AMOUNT_MISMATCH",
                    "O valor recebido deve ser igual ao valor da cobrança (pagamento parcial não é aceito).");
        }
        UUID actor = TenantContext.userId();
        Payment payment = payments.insert(new Payment(UuidV7.generate(), tenantId,
                numbers.nextPayment(tenantId, calendar.today(tenantId).getYear()), charge.id(), charge.studentId(),
                charge.branchId(), charge.amount(), charge.currency(), method, paidAt,
                BillingJournal.trimToNull(request.reference()), BillingJournal.trimToNull(request.notes()),
                PaymentStatus.CONFIRMED, actor, null, null, null, now));
        Charge saved = charges.updateStatus(charge.paid(paidAt));
        journal.record(tenantId, "PAYMENT_REGISTERED", "charge", saved.id(), "HIGH", charge.status().name(),
                saved.status().name(), Map.of("chargeNumber", saved.chargeNumber(),
                        "paymentId", payment.id().toString(),
                        "paymentNumber", payment.paymentNumber(),
                        "amount", payment.amount().toPlainString(),
                        "method", method.name()));
        return loader.detail(tenantId, saved);
    }
}
