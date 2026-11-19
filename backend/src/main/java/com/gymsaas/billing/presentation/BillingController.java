package com.gymsaas.billing.presentation;

import com.gymsaas.billing.application.BillingDtos;
import com.gymsaas.billing.application.usecase.BillingSummaryUseCase;
import com.gymsaas.shared.infrastructure.tenant.TenantContext;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.UUID;

@RestController
@RequestMapping("/api/billing")
@Tag(name = "Financeiro — resumo", description = "Indicadores financeiros do período")
public class BillingController {

    private final BillingSummaryUseCase summary;

    public BillingController(BillingSummaryUseCase summary) {
        this.summary = summary;
    }

    @GetMapping("/summary")
    @Operation(summary = "A receber, vencido, recebido por forma de pagamento e estornos (padrão: mês corrente)")
    @PreAuthorize("hasAuthority('billing:reports')")
    public BillingDtos.SummaryView summary(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(required = false) UUID branchId) {
        return summary.execute(TenantContext.requireTenantId(), from, to, branchId);
    }
}
