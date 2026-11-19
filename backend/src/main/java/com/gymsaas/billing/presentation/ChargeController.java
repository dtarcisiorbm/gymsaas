package com.gymsaas.billing.presentation;

import com.gymsaas.billing.application.BillingDtos;
import com.gymsaas.billing.application.usecase.CancelChargeUseCase;
import com.gymsaas.billing.application.usecase.CreateManualChargeUseCase;
import com.gymsaas.billing.application.usecase.FindChargeByIdUseCase;
import com.gymsaas.billing.application.usecase.ListChargesUseCase;
import com.gymsaas.billing.application.usecase.RegisterPaymentUseCase;
import com.gymsaas.shared.domain.PageResult;
import com.gymsaas.shared.infrastructure.tenant.TenantContext;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.UUID;

/** Tenant from the authenticated context only. Reading also accepts the legacy {@code payment:read}. */
@RestController
@RequestMapping("/api/charges")
@Tag(name = "Financeiro — cobranças", description = "Cobranças, recebimentos manuais e cancelamento")
public class ChargeController {

    static final String CAN_READ = "hasAnyAuthority('charges:read', 'payment:read')";

    private final ListChargesUseCase list;
    private final FindChargeByIdUseCase find;
    private final CreateManualChargeUseCase create;
    private final CancelChargeUseCase cancel;
    private final RegisterPaymentUseCase registerPayment;

    public ChargeController(ListChargesUseCase list, FindChargeByIdUseCase find, CreateManualChargeUseCase create,
                            CancelChargeUseCase cancel, RegisterPaymentUseCase registerPayment) {
        this.list = list;
        this.find = find;
        this.create = create;
        this.cancel = cancel;
        this.registerPayment = registerPayment;
    }

    @GetMapping
    @Operation(summary = "Lista cobranças (status OPEN, PAID, CANCELLED ou OVERDUE; aluno, matrícula, unidade, vencimento)")
    @PreAuthorize(CAN_READ)
    public PageResult<BillingDtos.ChargeView> list(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) UUID studentId,
            @RequestParam(required = false) UUID membershipId,
            @RequestParam(required = false) UUID branchId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dueFrom,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dueTo,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String sort,
            @RequestParam(required = false) String direction) {
        return list.execute(TenantContext.requireTenantId(), new BillingDtos.ChargeFilter(search, status, studentId,
                membershipId, branchId, dueFrom, dueTo, page, size, sort, direction));
    }

    @PostMapping
    @Operation(summary = "Lança cobrança avulsa")
    @PreAuthorize("hasAuthority('charges:create')")
    public ResponseEntity<BillingDtos.ChargeDetail> create(@Valid @RequestBody BillingDtos.CreateChargeRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(create.execute(TenantContext.requireTenantId(), request));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Detalhe da cobrança com recebimentos")
    @PreAuthorize(CAN_READ)
    public BillingDtos.ChargeDetail get(@PathVariable UUID id) {
        return find.execute(TenantContext.requireTenantId(), id);
    }

    @PostMapping("/{id}/cancel")
    @Operation(summary = "Cancela cobrança em aberto (motivo obrigatório)")
    @PreAuthorize("hasAuthority('charges:cancel')")
    public BillingDtos.ChargeDetail cancel(@PathVariable UUID id,
                                           @Valid @RequestBody BillingDtos.ReasonRequest request) {
        return cancel.execute(TenantContext.requireTenantId(), id, request);
    }

    @PostMapping("/{id}/payments")
    @Operation(summary = "Registra recebimento manual (valor integral)")
    @PreAuthorize("hasAuthority('payments:register')")
    public ResponseEntity<BillingDtos.ChargeDetail> registerPayment(
            @PathVariable UUID id, @Valid @RequestBody BillingDtos.RegisterPaymentRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(registerPayment.execute(TenantContext.requireTenantId(), id, request));
    }
}
