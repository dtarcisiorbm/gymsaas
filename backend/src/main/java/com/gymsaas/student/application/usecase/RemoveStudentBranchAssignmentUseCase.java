package com.gymsaas.student.application.usecase;

import com.gymsaas.audit.application.AuditService;
import com.gymsaas.shared.application.ApiException;
import com.gymsaas.student.application.StudentGuard;
import com.gymsaas.student.domain.StudentBranchAssignment;
import com.gymsaas.student.domain.StudentBranchAssignmentRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
public class RemoveStudentBranchAssignmentUseCase {

    private final StudentGuard guard;
    private final StudentBranchAssignmentRepository assignments;
    private final AuditService audit;

    public RemoveStudentBranchAssignmentUseCase(StudentGuard guard,
                                                StudentBranchAssignmentRepository assignments,
                                                AuditService audit) {
        this.guard = guard;
        this.assignments = assignments;
        this.audit = audit;
    }

    @Transactional
    public void execute(UUID tenantId, UUID studentId, UUID assignmentId) {
        guard.requireStudent(tenantId, studentId);
        StudentBranchAssignment current = assignments.findById(tenantId, studentId, assignmentId)
                .orElseThrow(() -> ApiException.notFound("ASSIGNMENT_NOT_FOUND",
                        "Vínculo com unidade não encontrado."));

        Instant now = Instant.now();
        assignments.end(tenantId, studentId, assignmentId, now, "Removido");

        if (current.primary()) {
            List<StudentBranchAssignment> remaining = assignments.findActive(tenantId, studentId)
                    .stream()
                    .filter(assignment -> !assignment.id().equals(assignmentId))
                    .sorted(Comparator.comparing(StudentBranchAssignment::startedAt))
                    .toList();
            if (!remaining.isEmpty()) {
                StudentBranchAssignment promoted = remaining.get(0);
                assignments.save(new StudentBranchAssignment(promoted.id(), promoted.tenantId(),
                        promoted.studentId(), promoted.branchId(), true, promoted.startedAt(),
                        promoted.endedAt(), promoted.transferReason(), promoted.assignedBy(),
                        promoted.createdAt(), now));
            }
        }

        audit.recordForTenant(tenantId, "STUDENT_BRANCH_REMOVED", "student", studentId, "MEDIUM",
                Map.of("assignmentId", assignmentId.toString()), null, Map.of());
    }
}
