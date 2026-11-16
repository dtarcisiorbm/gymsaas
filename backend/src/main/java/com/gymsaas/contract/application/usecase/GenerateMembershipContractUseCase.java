package com.gymsaas.contract.application.usecase;

import com.gymsaas.branch.domain.Branch;
import com.gymsaas.branch.domain.BranchRepository;
import com.gymsaas.contract.application.ContractAssembler;
import com.gymsaas.contract.application.ContractContentFactory;
import com.gymsaas.contract.application.ContractDtos;
import com.gymsaas.contract.domain.ContractDocumentRenderer;
import com.gymsaas.contract.domain.ContractDocumentStorage;
import com.gymsaas.contract.domain.ContractRepository;
import com.gymsaas.contract.domain.ContractStatus;
import com.gymsaas.contract.domain.MembershipContract;
import com.gymsaas.contract.domain.StoredDocument;
import com.gymsaas.membership.application.MembershipGuard;
import com.gymsaas.membership.application.MembershipJournal;
import com.gymsaas.membership.domain.Membership;
import com.gymsaas.membership.domain.MembershipEventType;
import com.gymsaas.shared.application.ApiException;
import com.gymsaas.shared.domain.BrDocuments;
import com.gymsaas.shared.domain.Sha256;
import com.gymsaas.shared.domain.UuidV7;
import com.gymsaas.shared.infrastructure.tenant.TenantContext;
import com.gymsaas.student.domain.Student;
import com.gymsaas.student.domain.StudentDocumentRepository;
import com.gymsaas.student.domain.StudentRepository;
import com.gymsaas.tenant.application.TenantCalendar;
import com.gymsaas.tenant.domain.Tenant;
import com.gymsaas.tenant.domain.TenantRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Generates a new immutable contract version from the membership snapshot.
 * A previous GENERATED (not accepted) version is superseded; an ACCEPTED
 * contract can never be regenerated. The PDF goes to external storage and
 * only its key/hash/size are persisted.
 */
@Service
public class GenerateMembershipContractUseCase {

    private final MembershipGuard membershipGuard;
    private final MembershipJournal journal;
    private final ContractRepository contracts;
    private final ContractContentFactory contentFactory;
    private final ContractDocumentRenderer renderer;
    private final ContractDocumentStorage storage;
    private final TenantRepository tenants;
    private final StudentRepository students;
    private final StudentDocumentRepository documents;
    private final BranchRepository branches;
    private final TenantCalendar calendar;

    public GenerateMembershipContractUseCase(MembershipGuard membershipGuard, MembershipJournal journal,
                                             ContractRepository contracts, ContractContentFactory contentFactory,
                                             ContractDocumentRenderer renderer, ContractDocumentStorage storage,
                                             TenantRepository tenants, StudentRepository students,
                                             StudentDocumentRepository documents, BranchRepository branches,
                                             TenantCalendar calendar) {
        this.membershipGuard = membershipGuard;
        this.journal = journal;
        this.contracts = contracts;
        this.contentFactory = contentFactory;
        this.renderer = renderer;
        this.storage = storage;
        this.tenants = tenants;
        this.students = students;
        this.documents = documents;
        this.branches = branches;
        this.calendar = calendar;
    }

    @Transactional
    public ContractDtos.ContractView execute(UUID tenantId, UUID membershipId) {
        Membership membership = membershipGuard.requireMembership(tenantId, membershipId);
        if (!membership.status().open()) {
            throw ApiException.conflict("CONTRACT_NOT_ALLOWED", "Matrícula encerrada não gera novo contrato.");
        }
        List<MembershipContract> existing = contracts.findByMembership(tenantId, membershipId);
        Optional<MembershipContract> current = existing.stream()
                .filter(contract -> contract.status() != ContractStatus.SUPERSEDED).findFirst();
        if (current.isPresent() && current.get().status() == ContractStatus.ACCEPTED) {
            throw ApiException.conflict("CONTRACT_ALREADY_ACCEPTED",
                    "O contrato desta matrícula já foi aceito e não pode ser regenerado.");
        }
        int version = existing.stream().mapToInt(MembershipContract::contractVersion).max().orElse(0) + 1;

        Tenant tenant = tenants.findById(tenantId)
                .orElseThrow(() -> ApiException.notFound("TENANT_NOT_FOUND", "Academia não encontrada."));
        Student student = students.findById(tenantId, membership.studentId())
                .orElseThrow(() -> ApiException.conflict("STUDENT_NOT_FOUND", "Aluno da matrícula não encontrado."));
        String maskedCpf = documents.findCpfByStudentIds(tenantId, List.of(student.id())).stream().findFirst()
                .map(document -> BrDocuments.maskCpf(document.normalizedNumber())).orElse(null);
        Branch branch = branches.findById(tenantId, membership.branchId()).orElse(null);

        Instant now = calendar.now();
        String content = contentFactory.render(new ContractContentFactory.ContractData(version, now,
                calendar.zone(tenantId), tenant, branch, student, maskedCpf, membership,
                featureBranchNames(tenantId, membership)));
        UUID contractId = UuidV7.generate();
        byte[] document = renderer.render("Contrato de matrícula " + membership.registrationNumber(), content);
        StoredDocument stored = storage.store(tenantId + "/" + membershipId + "/" + contractId + "-v" + version + ".pdf",
                document, renderer.contentType());

        current.ifPresent(previous -> contracts.save(previous.withStatus(ContractStatus.SUPERSEDED, now)));
        MembershipContract saved = contracts.save(new MembershipContract(contractId, tenantId, membershipId, version,
                ContractContentFactory.TEMPLATE_VERSION, ContractStatus.GENERATED, content, Sha256.hex(content),
                stored.storageKey(), stored.sha256(), stored.sizeBytes(), stored.contentType(),
                TenantContext.userId(), now, null));

        journal.record(membership, MembershipEventType.CONTRACT_GENERATED, membership.status(), membership.status(),
                null, Map.of("contractId", saved.id().toString(), "contractVersion", version,
                        "contentSha256", saved.contentSha256()), now, "MEDIUM");
        return ContractAssembler.view(saved, null);
    }

    @SuppressWarnings("unchecked")
    private Map<UUID, String> featureBranchNames(UUID tenantId, Membership membership) {
        Object features = membership.conditions().snapshot().get("features");
        if (!(features instanceof List<?> list)) {
            return Map.of();
        }
        List<UUID> ids = list.stream()
                .filter(item -> item instanceof Map<?, ?>)
                .map(item -> ((Map<String, Object>) item).get("branchId"))
                .filter(Objects::nonNull)
                .map(value -> UUID.fromString(value.toString()))
                .distinct().toList();
        return branches.findAllByIds(tenantId, ids).stream().collect(Collectors.toMap(Branch::id, Branch::name));
    }
}
