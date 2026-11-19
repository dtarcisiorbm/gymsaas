package com.gymsaas.billing.presentation;

import com.gymsaas.billing.application.BillingDtos;
import com.gymsaas.billing.application.usecase.RefundPaymentUseCase;
import com.gymsaas.shared.infrastructure.tenant.TenantContext;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/payments")
@Tag(name = "Financeiro — recebimentos", description = "Estorno de recebimentos")
public class PaymentController {

    private final RefundPaymentUseCase refund;

    public PaymentController(RefundPaymentUseCase refund) {
        this.refund = refund;
    }

    @PostMapping("/{id}/refund")
    @Operation(summary = "Estorna recebimento (motivo obrigatório); a cobrança volta a ficar em aberto")
    @PreAuthorize("hasAuthority('payments:refund')")
    public BillingDtos.ChargeDetail refund(@PathVariable UUID id,
                                           @Valid @RequestBody BillingDtos.ReasonRequest request) {
        return refund.execute(TenantContext.requireTenantId(), id, request);
    }
}
