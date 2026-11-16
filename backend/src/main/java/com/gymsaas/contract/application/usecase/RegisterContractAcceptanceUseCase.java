package com.gymsaas.contract.application.usecase;

import com.gymsaas.contract.application.ContractAssembler;
import com.gymsaas.contract.application.ContractContentFactory;
import com.gymsaas.contract.application.ContractDtos;
import com.gymsaas.contract.application.ContractGuard;
import com.gymsaas.contract.domain.AcceptanceChannel;
import com.gymsaas.contract.domain.ContractAcceptance;
import com.gymsaas.contract.domain.ContractAcceptanceRepository;
import com.gymsaas.contract.domain.ContractRepository;
import com.gymsaas.contract.domain.ContractStatus;
import com.gymsaas.contract.domain.MembershipContract;
import com.gymsaas.membership.application.MembershipGuard;
import com.gymsaas.membership.application.MembershipJournal;
import com.gymsaas.membership.domain.Membership;
import com.gymsaas.membership.domain.MembershipEventType;
import com.gymsaas.shared.application.ApiException;
import com.gymsaas.shared.domain.BrDocuments;
import com.gymsaas.shared.domain.UuidV7;
import com.gymsaas.shared.infrastructure.tenant.TenantContext;
import com.gymsaas.student.domain.StudentDocumentRepository;
import com.gymsaas.tenant.application.TenantCalendar;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Registers a SIMPLE in-person acceptance of a generated contract. The
 * operator must send the SHA-256 of the content shown to the student; if it
 * differs from the stored content the acceptance is refused.
 */
@Service
public class RegisterContractAcceptanceUseCase {

    private static final int USER_AGENT_MAX = 500;
    private static final int IP_MAX = 45;

    private final ContractGuard guard;
    private final ContractRepository contracts;
    private final ContractAcceptanceRepository acceptances;
    private final MembershipGuard membershipGuard;
    private final MembershipJournal journal;
    private final StudentDocumentRepository documents;
    private final TenantCalendar calendar;

    public RegisterContractAcceptanceUseCase(ContractGuard guard, ContractRepository contracts,
                                             ContractAcceptanceRepository acceptances,
                                             MembershipGuard membershipGuard, MembershipJournal journal,
                                             StudentDocumentRepository documents, TenantCalendar calendar) {
        this.guard = guard;
        this.contracts = contracts;
        this.acceptances = acceptances;
        this.membershipGuard = membershipGuard;
        this.journal = journal;
        this.documents = documents;
        this.calendar = calendar;
    }

    @Transactional
    public ContractDtos.ContractView execute(UUID tenantId, UUID contractId,
                                             ContractDtos.RegisterAcceptanceRequest request,
                                             String ipAddress, String userAgent) {
        MembershipContract contract = guard.requireContract(tenantId, contractId);
        if (contract.status() == ContractStatus.ACCEPTED) {
            throw ApiException.conflict("CONTRACT_ALREADY_ACCEPTED", "Este contrato já foi aceito.");
        }
        if (contract.status() == ContractStatus.SUPERSEDED) {
            throw ApiException.conflict("CONTRACT_SUPERSEDED", "Esta versão foi substituída por uma mais recente.");
        }
        if (!contract.contentSha256().equals(request.contentSha256())) {
            throw ApiException.conflict("CONTRACT_CONTENT_MISMATCH",
                    "O conteúdo exibido não corresponde à versão armazenada do contrato.");
        }
        Membership membership = membershipGuard.requireMembership(tenantId, contract.membershipId());
        if (!membership.status().open()) {
            throw ApiException.conflict("CONTRACT_NOT_ALLOWED", "Matrícula encerrada não aceita registro de aceite.");
        }
        String maskedCpf = documents.findCpfByStudentIds(tenantId, List.of(membership.studentId())).stream()
                .findFirst().map(document -> BrDocuments.maskCpf(document.normalizedNumber())).orElse(null);

        Instant now = calendar.now();
        ContractAcceptance acceptance = acceptances.save(new ContractAcceptance(UuidV7.generate(), tenantId,
                contract.id(), membership.id(), membership.studentId(), now, AcceptanceChannel.IN_PERSON,
                request.acceptorName().trim(), maskedCpf, contract.contentSha256(),
                ContractContentFactory.ACCEPTANCE_DECLARATION, truncate(ipAddress, IP_MAX),
                truncate(userAgent, USER_AGENT_MAX), TenantContext.userId(), now));
        MembershipContract accepted = contracts.save(contract.withStatus(ContractStatus.ACCEPTED, now));

        journal.record(membership, MembershipEventType.CONTRACT_ACCEPTED, membership.status(), membership.status(),
                null, Map.of("contractId", contract.id().toString(), "contractVersion", contract.contractVersion(),
                        "acceptanceId", acceptance.id().toString(), "channel", acceptance.channel().name()),
                now, "HIGH");
        return ContractAssembler.view(accepted, acceptance);
    }

    private static String truncate(String value, int max) {
        if (value == null || value.isBlank()) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.length() <= max ? trimmed : trimmed.substring(0, max);
    }
}
