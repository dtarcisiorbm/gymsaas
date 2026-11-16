package com.gymsaas.contract.presentation;

import com.gymsaas.contract.application.ContractDtos;
import com.gymsaas.contract.application.usecase.DownloadContractDocumentUseCase;
import com.gymsaas.contract.application.usecase.FindContractByIdUseCase;
import com.gymsaas.contract.application.usecase.GenerateMembershipContractUseCase;
import com.gymsaas.contract.application.usecase.GetContractHistoryUseCase;
import com.gymsaas.contract.application.usecase.RegisterContractAcceptanceUseCase;
import com.gymsaas.shared.infrastructure.tenant.TenantContext;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.CacheControl;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@Tag(name = "Contratos", description = "Contratos de matrícula, PDF e registro de aceite simples")
public class ContractController {

    private final GenerateMembershipContractUseCase generate;
    private final GetContractHistoryUseCase history;
    private final FindContractByIdUseCase find;
    private final RegisterContractAcceptanceUseCase accept;
    private final DownloadContractDocumentUseCase download;

    public ContractController(GenerateMembershipContractUseCase generate, GetContractHistoryUseCase history,
                              FindContractByIdUseCase find, RegisterContractAcceptanceUseCase accept,
                              DownloadContractDocumentUseCase download) {
        this.generate = generate;
        this.history = history;
        this.find = find;
        this.accept = accept;
        this.download = download;
    }

    @PostMapping("/api/memberships/{membershipId}/contracts")
    @Operation(summary = "Gera nova versão do contrato (substitui versão não aceita)")
    @PreAuthorize("hasAuthority('contracts:generate')")
    public ResponseEntity<ContractDtos.ContractView> generate(@PathVariable UUID membershipId) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(generate.execute(TenantContext.requireTenantId(), membershipId));
    }

    @GetMapping("/api/memberships/{membershipId}/contracts")
    @Operation(summary = "Histórico de versões do contrato da matrícula")
    @PreAuthorize("hasAuthority('contracts:read')")
    public List<ContractDtos.ContractSummary> history(@PathVariable UUID membershipId) {
        return history.execute(TenantContext.requireTenantId(), membershipId);
    }

    @GetMapping("/api/contracts/{id}")
    @Operation(summary = "Conteúdo, hash e evidências de aceite do contrato")
    @PreAuthorize("hasAuthority('contracts:read')")
    public ContractDtos.ContractView get(@PathVariable UUID id) {
        return find.execute(TenantContext.requireTenantId(), id);
    }

    @GetMapping("/api/contracts/{id}/document")
    @Operation(summary = "Baixa o PDF (integridade verificada por SHA-256)")
    @PreAuthorize("hasAuthority('contracts:read')")
    public ResponseEntity<byte[]> document(@PathVariable UUID id) {
        ContractDtos.DocumentFile file = download.execute(TenantContext.requireTenantId(), id);
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(file.contentType()))
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        ContentDisposition.attachment().filename(file.filename()).build().toString())
                .cacheControl(CacheControl.noStore())
                .body(file.content());
    }

    @PostMapping("/api/contracts/{id}/acceptance")
    @Operation(summary = "Registra aceite simples presencial (não é assinatura digital qualificada)")
    @PreAuthorize("hasAuthority('contracts:register_acceptance')")
    public ResponseEntity<ContractDtos.ContractView> accept(
            @PathVariable UUID id, @Valid @RequestBody ContractDtos.RegisterAcceptanceRequest request,
            HttpServletRequest httpRequest) {
        return ResponseEntity.status(HttpStatus.CREATED).body(accept.execute(TenantContext.requireTenantId(), id,
                request, httpRequest.getRemoteAddr(), httpRequest.getHeader(HttpHeaders.USER_AGENT)));
    }
}
