package com.gymsaas.membership.presentation;

import com.gymsaas.membership.application.MembershipDtos;
import com.gymsaas.membership.application.usecase.CancelMembershipUseCase;
import com.gymsaas.membership.application.usecase.CreateMembershipUseCase;
import com.gymsaas.membership.application.usecase.FindMembershipByIdUseCase;
import com.gymsaas.membership.application.usecase.FreezeMembershipUseCase;
import com.gymsaas.membership.application.usecase.GetMembershipHistoryUseCase;
import com.gymsaas.membership.application.usecase.ListMembershipsUseCase;
import com.gymsaas.membership.application.usecase.QuoteMembershipUseCase;
import com.gymsaas.membership.application.usecase.ReactivateMembershipUseCase;
import com.gymsaas.membership.application.usecase.RenewMembershipUseCase;
import com.gymsaas.membership.application.usecase.SuspendMembershipUseCase;
import com.gymsaas.membership.application.usecase.UnfreezeMembershipUseCase;
import com.gymsaas.membership.application.usecase.UpdateMembershipUseCase;
import com.gymsaas.shared.domain.PageResult;
import com.gymsaas.shared.infrastructure.tenant.TenantContext;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/**
 * Tenant from the authenticated context only. Requests carry no prices; the
 * discount permission is derived from the caller's authorities.
 */
@RestController
@RequestMapping("/api/memberships")
@Tag(name = "Matrículas", description = "Matrículas, renovação, cancelamento, congelamento e histórico")
public class MembershipController {

    private static final String APPLY_DISCOUNT = "memberships:apply_discount";

    private final QuoteMembershipUseCase quote;
    private final CreateMembershipUseCase create;
    private final FindMembershipByIdUseCase find;
    private final ListMembershipsUseCase list;
    private final UpdateMembershipUseCase update;
    private final RenewMembershipUseCase renew;
    private final CancelMembershipUseCase cancel;
    private final FreezeMembershipUseCase freeze;
    private final UnfreezeMembershipUseCase unfreeze;
    private final SuspendMembershipUseCase suspend;
    private final ReactivateMembershipUseCase reactivate;
    private final GetMembershipHistoryUseCase history;

    public MembershipController(QuoteMembershipUseCase quote, CreateMembershipUseCase create,
                                FindMembershipByIdUseCase find, ListMembershipsUseCase list,
                                UpdateMembershipUseCase update, RenewMembershipUseCase renew,
                                CancelMembershipUseCase cancel, FreezeMembershipUseCase freeze,
                                UnfreezeMembershipUseCase unfreeze, SuspendMembershipUseCase suspend,
                                ReactivateMembershipUseCase reactivate, GetMembershipHistoryUseCase history) {
        this.quote = quote;
        this.create = create;
        this.find = find;
        this.list = list;
        this.update = update;
        this.renew = renew;
        this.cancel = cancel;
        this.freeze = freeze;
        this.unfreeze = unfreeze;
        this.suspend = suspend;
        this.reactivate = reactivate;
        this.history = history;
    }

    @GetMapping
    @Operation(summary = "Lista matrículas (paginado; filtros por aluno, plano, unidade, status e término)")
    @PreAuthorize("hasAuthority('memberships:read')")
    public PageResult<MembershipDtos.MembershipSummary> list(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) UUID studentId,
            @RequestParam(required = false) UUID planId,
            @RequestParam(required = false) UUID branchId,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endingFrom,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endingTo,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String sort,
            @RequestParam(required = false) String direction) {
        return list.execute(TenantContext.requireTenantId(), new MembershipDtos.MembershipFilter(search, studentId,
                planId, branchId, status, endingFrom, endingTo, page, size, sort, direction));
    }

    @PostMapping("/quote")
    @Operation(summary = "Resumo da contratação calculado pelo backend (não persiste)")
    @PreAuthorize("hasAuthority('memberships:create')")
    public MembershipDtos.QuoteView quote(@Valid @RequestBody MembershipDtos.CreateMembershipRequest request,
                                          Authentication authentication) {
        return quote.execute(TenantContext.requireTenantId(), request, canApplyDiscount(authentication));
    }

    @PostMapping
    @Operation(summary = "Cria matrícula com preço vigente do plano (snapshot imutável)")
    @PreAuthorize("hasAuthority('memberships:create')")
    public ResponseEntity<MembershipDtos.MembershipDetail> create(
            @Valid @RequestBody MembershipDtos.CreateMembershipRequest request, Authentication authentication) {
        return ResponseEntity.status(HttpStatus.CREATED).body(
                create.execute(TenantContext.requireTenantId(), request, canApplyDiscount(authentication)));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Detalhe da matrícula com condições contratadas e congelamentos")
    @PreAuthorize("hasAuthority('memberships:read')")
    public MembershipDtos.MembershipDetail get(@PathVariable UUID id) {
        return find.execute(TenantContext.requireTenantId(), id);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Atualiza dados operacionais (unidade e observações)")
    @PreAuthorize("hasAuthority('memberships:update')")
    public MembershipDtos.MembershipDetail update(@PathVariable UUID id,
                                                  @Valid @RequestBody MembershipDtos.UpdateMembershipRequest request) {
        return update.execute(TenantContext.requireTenantId(), id, request);
    }

    @PostMapping("/{id}/renew")
    @Operation(summary = "Renova criando nova matrícula vinculada (preço vigente na data de início)")
    @PreAuthorize("hasAuthority('memberships:renew')")
    public ResponseEntity<MembershipDtos.MembershipDetail> renew(
            @PathVariable UUID id, @Valid @RequestBody MembershipDtos.RenewMembershipRequest request,
            Authentication authentication) {
        return ResponseEntity.status(HttpStatus.CREATED).body(
                renew.execute(TenantContext.requireTenantId(), id, request, canApplyDiscount(authentication)));
    }

    @PostMapping("/{id}/cancel")
    @Operation(summary = "Cancela com motivo, data e responsável")
    @PreAuthorize("hasAuthority('memberships:cancel')")
    public MembershipDtos.MembershipDetail cancel(@PathVariable UUID id,
                                                  @Valid @RequestBody MembershipDtos.ReasonRequest request) {
        return cancel.execute(TenantContext.requireTenantId(), id, request);
    }

    @PostMapping("/{id}/freeze")
    @Operation(summary = "Congela a partir de hoje até a data informada (estende a vigência)")
    @PreAuthorize("hasAuthority('memberships:freeze')")
    public MembershipDtos.MembershipDetail freeze(@PathVariable UUID id,
                                                  @Valid @RequestBody MembershipDtos.FreezeMembershipRequest request) {
        return freeze.execute(TenantContext.requireTenantId(), id, request);
    }

    @PostMapping("/{id}/unfreeze")
    @Operation(summary = "Descongela antecipadamente (devolve os dias não usados)")
    @PreAuthorize("hasAuthority('memberships:freeze')")
    public MembershipDtos.MembershipDetail unfreeze(
            @PathVariable UUID id, @Valid @RequestBody(required = false) MembershipDtos.OptionalReasonRequest request) {
        return unfreeze.execute(TenantContext.requireTenantId(), id, request);
    }

    @PostMapping("/{id}/suspend")
    @Operation(summary = "Suspende administrativamente (não estende a vigência)")
    @PreAuthorize("hasAuthority('memberships:suspend')")
    public MembershipDtos.MembershipDetail suspend(@PathVariable UUID id,
                                                   @Valid @RequestBody MembershipDtos.ReasonRequest request) {
        return suspend.execute(TenantContext.requireTenantId(), id, request);
    }

    @PostMapping("/{id}/reactivate")
    @Operation(summary = "Reativa matrícula suspensa")
    @PreAuthorize("hasAuthority('memberships:suspend')")
    public MembershipDtos.MembershipDetail reactivate(@PathVariable UUID id,
                                                      @Valid @RequestBody MembershipDtos.ReasonRequest request) {
        return reactivate.execute(TenantContext.requireTenantId(), id, request);
    }

    @GetMapping("/{id}/history")
    @Operation(summary = "Histórico completo da matrícula")
    @PreAuthorize("hasAuthority('memberships:read')")
    public List<MembershipDtos.HistoryEntryView> history(@PathVariable UUID id) {
        return history.execute(TenantContext.requireTenantId(), id);
    }

    private static boolean canApplyDiscount(Authentication authentication) {
        return authentication != null && authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .anyMatch(APPLY_DISCOUNT::equals);
    }
}
