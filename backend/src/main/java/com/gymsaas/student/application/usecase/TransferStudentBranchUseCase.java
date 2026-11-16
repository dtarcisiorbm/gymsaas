package com.gymsaas.student.application.usecase;

import com.gymsaas.audit.application.AuditService;
import com.gymsaas.branch.domain.Branch;
import com.gymsaas.shared.application.ApiException;
import com.gymsaas.shared.domain.UuidV7;
import com.gymsaas.shared.infrastructure.tenant.TenantContext;
import com.gymsaas.student.application.StudentDtos;
import com.gymsaas.student.application.StudentGuard;
import com.gymsaas.student.domain.StudentBranchAssignment;
import com.gymsaas.student.domain.StudentBranchAssignmentRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Service
public class TransferStudentBranchUseCase {

    private final StudentGuard guard;
    private final StudentBranchAssignmentRepository assignments;
    private final AuditService audit;

    public TransferStudentBranchUseCase(StudentGuard guard,
                                        StudentBranchAssignmentRepository assignments,
                                        AuditService audit) {
        this.guard = guard;
        this.assignments = assignments;
        this.audit = audit;
    }

    @Transactional
    public StudentDtos.BranchAssignmentView execute(UUID tenantId, UUID studentId,
                                                    StudentDtos.TransferBranchRequest request) {
        guard.requireStudent(tenantId, studentId);
        Branch target = guard.requireBranch(tenantId, request.branchId());

        Optional<StudentBranchAssignment> currentPrimary =
                assignments.findActivePrimary(tenantId, studentId);
        if (currentPrimary.isPresent() && target.id().equals(currentPrimary.get().branchId())) {
            throw ApiException.conflict("SAME_BRANCH",
                    "O aluno já está vinculado a esta unidade como principal.");
        }

        Instant now = Instant.now();
        String reason = StudentGuard.trimToNull(request.reason());
        String closureReason = reason == null ? "Transferência de unidade" : reason;

        currentPrimary.ifPresent(primary ->
                assignments.end(tenantId, studentId, primary.id(), now, closureReason));

        List<StudentBranchAssignment> active = assignments.findActive(tenantId, studentId);
        UUID currentPrimaryId = currentPrimary.map(StudentBranchAssignment::id).orElse(null);
        for (StudentBranchAssignment assignment : active) {
            if (!assignment.id().equals(currentPrimaryId)) {
                assignments.end(tenantId, studentId, assignment.id(), now, closureReason);
            }
        }
        assignments.clearPrimary(tenantId, studentId);

        StudentBranchAssignment saved = assignments.save(new StudentBranchAssignment(
                UuidV7.generate(), tenantId, studentId, target.id(), true, now, null, reason,
                TenantContext.userId(), now, now));

        audit.recordForTenant(tenantId, "STUDENT_BRANCH_TRANSFERRED", "student", studentId, "MEDIUM",
                null, Map.of("branchId", target.id().toString()), Map.of());

        return new StudentDtos.BranchAssignmentView(saved.id(), target.id(), target.name(),
                saved.primary(), saved.startedAt(), saved.endedAt(), saved.transferReason());
    }
}
