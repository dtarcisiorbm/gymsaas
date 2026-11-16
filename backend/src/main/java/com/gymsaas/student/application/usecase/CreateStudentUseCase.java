package com.gymsaas.student.application.usecase;

import com.gymsaas.audit.application.AuditService;
import com.gymsaas.branch.domain.Branch;
import com.gymsaas.shared.domain.UuidV7;
import com.gymsaas.shared.infrastructure.tenant.TenantContext;
import com.gymsaas.student.application.StudentAssembler;
import com.gymsaas.student.application.StudentDtos;
import com.gymsaas.student.application.StudentGuard;
import com.gymsaas.student.domain.Student;
import com.gymsaas.student.domain.StudentAddress;
import com.gymsaas.student.domain.StudentAddressRepository;
import com.gymsaas.student.domain.StudentBranchAssignment;
import com.gymsaas.student.domain.StudentBranchAssignmentRepository;
import com.gymsaas.student.domain.StudentContact;
import com.gymsaas.student.domain.StudentContactRepository;
import com.gymsaas.student.domain.StudentDocument;
import com.gymsaas.student.domain.StudentDocumentRepository;
import com.gymsaas.student.domain.StudentRegistrationGenerator;
import com.gymsaas.student.domain.StudentRepository;
import com.gymsaas.student.domain.StudentStatus;
import com.gymsaas.student.domain.StudentStatusEntry;
import com.gymsaas.student.domain.StudentStatusHistoryRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
public class CreateStudentUseCase {

    private final StudentGuard guard;
    private final StudentRepository students;
    private final StudentRegistrationGenerator registration;
    private final StudentDocumentRepository documents;
    private final StudentContactRepository contacts;
    private final StudentAddressRepository addresses;
    private final StudentBranchAssignmentRepository assignments;
    private final StudentStatusHistoryRepository statusHistory;
    private final AuditService audit;

    public CreateStudentUseCase(StudentGuard guard, StudentRepository students,
                                StudentRegistrationGenerator registration,
                                StudentDocumentRepository documents, StudentContactRepository contacts,
                                StudentAddressRepository addresses,
                                StudentBranchAssignmentRepository assignments,
                                StudentStatusHistoryRepository statusHistory, AuditService audit) {
        this.guard = guard;
        this.students = students;
        this.registration = registration;
        this.documents = documents;
        this.contacts = contacts;
        this.addresses = addresses;
        this.assignments = assignments;
        this.statusHistory = statusHistory;
        this.audit = audit;
    }

    @Transactional
    public StudentDtos.StudentDetail execute(UUID tenantId, StudentDtos.CreateStudentRequest request) {
        StudentStatus status = guard.parseStatusOrDefault(request.status(), StudentStatus.PENDING);
        String cpf = request.cpf() == null || request.cpf().isBlank()
                ? null : guard.validateStudentCpf(tenantId, request.cpf(), null);
        Branch primaryBranch = request.primaryBranchId() == null
                ? null : guard.requireBranch(tenantId, request.primaryBranchId());

        Instant now = Instant.now();
        Student student = new Student(UuidV7.generate(), tenantId, registration.next(tenantId),
                request.name().trim(), StudentGuard.trimToNull(request.socialName()),
                request.birthDate(), StudentGuard.trimToNull(request.email()),
                StudentGuard.trimToNull(request.phone()), guard.normalizeGender(request.gender()),
                StudentGuard.trimToNull(request.photoUrl()), status,
                StudentGuard.trimToNull(request.notes()), 0, now, now, null);
        Student saved = students.save(student);

        List<StudentDocument> savedDocuments = new ArrayList<>();
        if (cpf != null) {
            savedDocuments.add(documents.save(new StudentDocument(UuidV7.generate(), tenantId,
                    saved.id(), "CPF", cpf, cpf, null, null, null, null, null, true, now, now, null)));
        }

        StudentAddress savedAddress = null;
        if (request.address() != null) {
            savedAddress = addresses.save(toAddress(tenantId, saved.id(), request.address(), now));
        }

        List<StudentContact> savedContacts = new ArrayList<>();
        if (request.contacts() != null && !request.contacts().isEmpty()) {
            boolean hasPrimary = request.contacts().stream()
                    .anyMatch(contact -> Boolean.TRUE.equals(contact.primary()));
            for (int index = 0; index < request.contacts().size(); index++) {
                StudentDtos.ContactRequest contact = request.contacts().get(index);
                boolean primary = hasPrimary ? Boolean.TRUE.equals(contact.primary()) : index == 0;
                savedContacts.add(contacts.save(new StudentContact(UuidV7.generate(), tenantId,
                        saved.id(), contact.name().trim(), StudentGuard.trimToNull(contact.relationship()),
                        StudentGuard.trimToNull(contact.phone()), StudentGuard.trimToNull(contact.email()),
                        primary, StudentGuard.trimToNull(contact.notes()), now, now, null)));
            }
        }

        List<StudentBranchAssignment> savedAssignments = new ArrayList<>();
        if (primaryBranch != null) {
            savedAssignments.add(assignments.save(new StudentBranchAssignment(UuidV7.generate(),
                    tenantId, saved.id(), primaryBranch.id(), true, now, null, null,
                    TenantContext.userId(), now, now)));
        }

        statusHistory.save(new StudentStatusEntry(UuidV7.generate(), tenantId, saved.id(), null,
                status, "Cadastro inicial", TenantContext.userId(), now));

        audit.recordForTenant(tenantId, "STUDENT_CREATED", "student", saved.id(), "MEDIUM",
                null, Map.of("name", saved.name(), "registrationNumber", saved.registrationNumber(),
                        "status", saved.status().name()), Map.of());

        Map<UUID, Branch> branchMap = primaryBranch == null
                ? Map.of() : Map.of(primaryBranch.id(), primaryBranch);
        return StudentAssembler.detail(saved, savedDocuments, savedContacts, savedAddress,
                savedAssignments, branchMap, false);
    }

    private StudentAddress toAddress(UUID tenantId, UUID studentId,
                                     StudentDtos.AddressRequest request, Instant now) {
        return new StudentAddress(UuidV7.generate(), tenantId, studentId,
                com.gymsaas.shared.domain.BrDocuments.zipCode(request.zipCode()),
                StudentGuard.trimToNull(request.street()), StudentGuard.trimToNull(request.number()),
                StudentGuard.trimToNull(request.complement()),
                StudentGuard.trimToNull(request.neighborhood()), StudentGuard.trimToNull(request.city()),
                StudentGuard.upperToNull(request.state()), now, now);
    }
}
