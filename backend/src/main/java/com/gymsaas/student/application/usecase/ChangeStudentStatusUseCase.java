package com.gymsaas.student.application.usecase;

import com.gymsaas.audit.application.AuditService;
import com.gymsaas.branch.domain.Branch;
import com.gymsaas.branch.domain.BranchRepository;
import com.gymsaas.shared.application.ApiException;
import com.gymsaas.shared.domain.UuidV7;
import com.gymsaas.shared.infrastructure.tenant.TenantContext;
import com.gymsaas.student.application.StudentAssembler;
import com.gymsaas.student.application.StudentDtos;
import com.gymsaas.student.application.StudentGuard;
import com.gymsaas.student.domain.Student;
import com.gymsaas.student.domain.StudentAddressRepository;
import com.gymsaas.student.domain.StudentBranchAssignment;
import com.gymsaas.student.domain.StudentBranchAssignmentRepository;
import com.gymsaas.student.domain.StudentContactRepository;
import com.gymsaas.student.domain.StudentDocumentRepository;
import com.gymsaas.student.domain.StudentRepository;
import com.gymsaas.student.domain.StudentStatus;
import com.gymsaas.student.domain.StudentStatusEntry;
import com.gymsaas.student.domain.StudentStatusHistoryRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class ChangeStudentStatusUseCase {

    private final StudentGuard guard;
    private final StudentRepository students;
    private final StudentStatusHistoryRepository statusHistory;
    private final StudentDocumentRepository documents;
    private final StudentContactRepository contacts;
    private final StudentAddressRepository addresses;
    private final StudentBranchAssignmentRepository assignments;
    private final BranchRepository branches;
    private final AuditService audit;

    public ChangeStudentStatusUseCase(StudentGuard guard, StudentRepository students,
                                      StudentStatusHistoryRepository statusHistory,
                                      StudentDocumentRepository documents,
                                      StudentContactRepository contacts,
                                      StudentAddressRepository addresses,
                                      StudentBranchAssignmentRepository assignments,
                                      BranchRepository branches, AuditService audit) {
        this.guard = guard;
        this.students = students;
        this.statusHistory = statusHistory;
        this.documents = documents;
        this.contacts = contacts;
        this.addresses = addresses;
        this.assignments = assignments;
        this.branches = branches;
        this.audit = audit;
    }

    @Transactional
    public StudentDtos.StudentDetail execute(UUID tenantId, UUID studentId,
                                             StudentDtos.ChangeStudentStatusRequest request) {
        Student current = guard.requireStudent(tenantId, studentId);
        StudentStatus target = guard.parseStatus(request.status());
        if (!current.status().canTransitionTo(target)) {
            throw ApiException.conflict("INVALID_STATUS_TRANSITION",
                    "Transição de status não permitida.");
        }

        Instant now = Instant.now();
        Student updated = new Student(current.id(), current.tenantId(), current.registrationNumber(),
                current.name(), current.socialName(), current.birthDate(), current.email(),
                current.phone(), current.gender(), current.photoUrl(), target, current.notes(),
                current.version(), current.createdAt(), now, current.deletedAt());
        Student saved = students.save(updated);

        statusHistory.save(new StudentStatusEntry(UuidV7.generate(), tenantId, studentId,
                current.status(), target, StudentGuard.trimToNull(request.reason()),
                TenantContext.userId(), now));

        audit.recordChange("STUDENT_STATUS_CHANGED", "student", saved.id(), "MEDIUM",
                Map.of("status", current.status().name()), Map.of("status", target.name()));

        List<StudentBranchAssignment> studentAssignments = assignments.findByStudent(tenantId, studentId);
        Map<UUID, Branch> branchMap = branches.findAllByIds(tenantId, studentAssignments.stream()
                        .map(StudentBranchAssignment::branchId).distinct().toList()).stream()
                .collect(Collectors.toMap(Branch::id, branch -> branch));

        return StudentAssembler.detail(saved, documents.findByStudent(tenantId, studentId),
                contacts.findByStudent(tenantId, studentId),
                addresses.findByStudent(tenantId, studentId).orElse(null),
                studentAssignments, branchMap, false);
    }
}
