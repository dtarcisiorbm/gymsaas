package com.gymsaas.student.application.usecase;

import com.gymsaas.audit.application.AuditService;
import com.gymsaas.branch.domain.Branch;
import com.gymsaas.branch.domain.BranchRepository;
import com.gymsaas.shared.domain.UuidV7;
import com.gymsaas.student.application.StudentAssembler;
import com.gymsaas.student.application.StudentDtos;
import com.gymsaas.student.application.StudentGuard;
import com.gymsaas.student.domain.Student;
import com.gymsaas.student.domain.StudentAddressRepository;
import com.gymsaas.student.domain.StudentBranchAssignment;
import com.gymsaas.student.domain.StudentBranchAssignmentRepository;
import com.gymsaas.student.domain.StudentContact;
import com.gymsaas.student.domain.StudentContactRepository;
import com.gymsaas.student.domain.StudentDocument;
import com.gymsaas.student.domain.StudentDocumentRepository;
import com.gymsaas.student.domain.StudentRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class UpdateStudentUseCase {

    private final StudentGuard guard;
    private final StudentRepository students;
    private final StudentDocumentRepository documents;
    private final StudentContactRepository contacts;
    private final StudentAddressRepository addresses;
    private final StudentBranchAssignmentRepository assignments;
    private final BranchRepository branches;
    private final AuditService audit;

    public UpdateStudentUseCase(StudentGuard guard, StudentRepository students,
                                StudentDocumentRepository documents, StudentContactRepository contacts,
                                StudentAddressRepository addresses,
                                StudentBranchAssignmentRepository assignments,
                                BranchRepository branches, AuditService audit) {
        this.guard = guard;
        this.students = students;
        this.documents = documents;
        this.contacts = contacts;
        this.addresses = addresses;
        this.assignments = assignments;
        this.branches = branches;
        this.audit = audit;
    }

    @Transactional
    public StudentDtos.StudentDetail execute(UUID tenantId, UUID studentId,
                                             StudentDtos.UpdateStudentRequest request) {
        Student current = guard.requireStudent(tenantId, studentId);
        String cpf = request.cpf() == null || request.cpf().isBlank()
                ? null : guard.validateStudentCpf(tenantId, request.cpf(), studentId);

        Student updated = new Student(current.id(), current.tenantId(), current.registrationNumber(),
                request.name().trim(), StudentGuard.trimToNull(request.socialName()),
                request.birthDate(), StudentGuard.trimToNull(request.email()),
                StudentGuard.trimToNull(request.phone()), guard.normalizeGender(request.gender()),
                StudentGuard.trimToNull(request.photoUrl()), current.status(),
                StudentGuard.trimToNull(request.notes()), current.version(),
                current.createdAt(), current.updatedAt(), current.deletedAt());
        Student saved = students.save(updated);

        if (cpf != null) {
            saveCpfDocument(tenantId, studentId, cpf);
        }

        List<StudentDocument> savedDocuments = documents.findByStudent(tenantId, studentId);
        List<StudentContact> savedContacts = contacts.findByStudent(tenantId, studentId);
        List<StudentBranchAssignment> savedAssignments = assignments.findByStudent(tenantId, studentId);
        Map<UUID, Branch> branchMap = branchMap(tenantId, savedAssignments);

        audit.recordChange("STUDENT_UPDATED", "student", saved.id(), "MEDIUM",
                Map.of("name", current.name()), Map.of("name", saved.name()));

        return StudentAssembler.detail(saved, savedDocuments, savedContacts,
                addresses.findByStudent(tenantId, studentId).orElse(null),
                savedAssignments, branchMap, false);
    }

    private void saveCpfDocument(UUID tenantId, UUID studentId, String cpf) {
        List<StudentDocument> current = documents.findByStudent(tenantId, studentId);
        Optional<StudentDocument> existing = current.stream()
                .filter(document -> "CPF".equals(document.type()))
                .findFirst();
        Instant now = Instant.now();
        StudentDocument document = new StudentDocument(
                existing.map(StudentDocument::id).orElseGet(UuidV7::generate), tenantId, studentId,
                "CPF", cpf, cpf, null, null, null, null, null, true, now, now, null);
        documents.save(document);
    }

    private Map<UUID, Branch> branchMap(UUID tenantId, List<StudentBranchAssignment> assignments) {
        List<UUID> ids = assignments.stream().map(StudentBranchAssignment::branchId).distinct().toList();
        return branches.findAllByIds(tenantId, ids).stream()
                .collect(Collectors.toMap(Branch::id, branch -> branch));
    }
}
