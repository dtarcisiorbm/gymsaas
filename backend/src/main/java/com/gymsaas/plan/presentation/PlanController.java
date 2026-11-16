package com.gymsaas.plan.presentation;

import com.gymsaas.plan.application.PlanDtos;
import com.gymsaas.plan.application.usecase.AddPlanPriceUseCase;
import com.gymsaas.plan.application.usecase.CancelPlanPriceUseCase;
import com.gymsaas.plan.application.usecase.ChangePlanStatusUseCase;
import com.gymsaas.plan.application.usecase.CreatePlanUseCase;
import com.gymsaas.plan.application.usecase.FindPlanByIdUseCase;
import com.gymsaas.plan.application.usecase.ListPlanPricesUseCase;
import com.gymsaas.plan.application.usecase.ListPlansUseCase;
import com.gymsaas.plan.application.usecase.ReplacePlanFeaturesUseCase;
import com.gymsaas.plan.application.usecase.UpdatePlanUseCase;
import com.gymsaas.shared.domain.PageResult;
import com.gymsaas.shared.infrastructure.tenant.TenantContext;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

/** Tenant always comes from the authenticated context, never from the request. */
@RestController
@RequestMapping("/api/plans")
@Tag(name = "Planos", description = "Planos, histórico de preços e benefícios")
public class PlanController {

    private final CreatePlanUseCase createPlan;
    private final UpdatePlanUseCase updatePlan;
    private final FindPlanByIdUseCase findPlan;
    private final ListPlansUseCase listPlans;
    private final ChangePlanStatusUseCase changeStatus;
    private final AddPlanPriceUseCase addPrice;
    private final CancelPlanPriceUseCase cancelPrice;
    private final ListPlanPricesUseCase listPrices;
    private final ReplacePlanFeaturesUseCase replaceFeatures;

    public PlanController(CreatePlanUseCase createPlan, UpdatePlanUseCase updatePlan, FindPlanByIdUseCase findPlan,
                          ListPlansUseCase listPlans, ChangePlanStatusUseCase changeStatus,
                          AddPlanPriceUseCase addPrice, CancelPlanPriceUseCase cancelPrice,
                          ListPlanPricesUseCase listPrices, ReplacePlanFeaturesUseCase replaceFeatures) {
        this.createPlan = createPlan;
        this.updatePlan = updatePlan;
        this.findPlan = findPlan;
        this.listPlans = listPlans;
        this.changeStatus = changeStatus;
        this.addPrice = addPrice;
        this.cancelPrice = cancelPrice;
        this.listPrices = listPrices;
        this.replaceFeatures = replaceFeatures;
    }

    @GetMapping
    @Operation(summary = "Lista planos (paginado, filtros por status e busca)")
    @PreAuthorize("hasAnyAuthority('plans:read','plan:read')")
    public PageResult<PlanDtos.PlanSummary> list(@RequestParam(required = false) String search,
                                                 @RequestParam(required = false) String status,
                                                 @RequestParam(defaultValue = "0") int page,
                                                 @RequestParam(defaultValue = "20") int size,
                                                 @RequestParam(required = false) String sort,
                                                 @RequestParam(required = false) String direction) {
        return listPlans.execute(TenantContext.requireTenantId(),
                new PlanDtos.PlanFilter(search, status, page, size, sort, direction));
    }

    @PostMapping
    @Operation(summary = "Cadastra plano (com preço inicial opcional)")
    @PreAuthorize("hasAnyAuthority('plans:create','plan:manage')")
    public ResponseEntity<PlanDtos.PlanDetail> create(@Valid @RequestBody PlanDtos.CreatePlanRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(createPlan.execute(TenantContext.requireTenantId(), request));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Detalhe do plano com preço vigente e benefícios")
    @PreAuthorize("hasAnyAuthority('plans:read','plan:read')")
    public PlanDtos.PlanDetail get(@PathVariable UUID id) {
        return findPlan.execute(TenantContext.requireTenantId(), id);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Atualiza definição do plano (não altera matrículas existentes)")
    @PreAuthorize("hasAnyAuthority('plans:update','plan:manage')")
    public PlanDtos.PlanDetail update(@PathVariable UUID id, @Valid @RequestBody PlanDtos.UpdatePlanRequest request) {
        return updatePlan.execute(TenantContext.requireTenantId(), id, request);
    }

    @PatchMapping("/{id}/status")
    @Operation(summary = "Ativa, inativa ou arquiva o plano")
    @PreAuthorize("hasAnyAuthority('plans:update','plan:manage')")
    public PlanDtos.PlanDetail changeStatus(@PathVariable UUID id,
                                            @Valid @RequestBody PlanDtos.ChangePlanStatusRequest request) {
        return changeStatus.execute(TenantContext.requireTenantId(), id, request);
    }

    @GetMapping("/{id}/prices")
    @Operation(summary = "Histórico completo de preços do plano")
    @PreAuthorize("hasAnyAuthority('plans:read','plan:read')")
    public List<PlanDtos.PriceView> prices(@PathVariable UUID id) {
        return listPrices.execute(TenantContext.requireTenantId(), id);
    }

    @PostMapping("/{id}/prices")
    @Operation(summary = "Programa novo preço (encerra o anterior; nunca altera matrículas)")
    @PreAuthorize("hasAnyAuthority('plans:manage_prices','plan:manage')")
    public ResponseEntity<PlanDtos.PriceView> addPrice(@PathVariable UUID id,
                                                       @Valid @RequestBody PlanDtos.PriceRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(addPrice.execute(TenantContext.requireTenantId(), id, request));
    }

    @PostMapping("/{id}/prices/{priceId}/cancel")
    @Operation(summary = "Cancela preço programado que ainda não entrou em vigor")
    @PreAuthorize("hasAnyAuthority('plans:manage_prices','plan:manage')")
    public PlanDtos.PriceView cancelPrice(@PathVariable UUID id, @PathVariable UUID priceId,
                                          @Valid @RequestBody PlanDtos.CancelPriceRequest request) {
        return cancelPrice.execute(TenantContext.requireTenantId(), id, priceId, request);
    }

    @PutMapping("/{id}/features")
    @Operation(summary = "Substitui os benefícios tipados do plano")
    @PreAuthorize("hasAnyAuthority('plans:update','plan:manage')")
    public PlanDtos.PlanDetail replaceFeatures(@PathVariable UUID id,
                                               @Valid @RequestBody PlanDtos.ReplaceFeaturesRequest request) {
        return replaceFeatures.execute(TenantContext.requireTenantId(), id, request);
    }
}
