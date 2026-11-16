package com.gymsaas.student.application.usecase;

import com.gymsaas.audit.application.AuditService;
import com.gymsaas.branch.domain.Branch;
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
import java.util.UUID;

@Service
public class AssignStudentToBranchUseCase {

    private final StudentGuard guard;
    private final StudentBranchAssignmentRepository assignments;
    private final AuditService audit;

    public AssignStudentToBranchUseCase(StudentGuard guard,
                                        StudentBranchAssignmentRepository assignments,
                                        AuditService audit) {
        this.guard = guard;
        this.assignments = assignments;
        this.audit = audit;
    }

    @Transactional
    public StudentDtos.BranchAssignmentView execute(UUID tenantId, UUID studentId,
                                                    StudentDtos.AssignBranchRequest request) {
        guard.requireStudent(tenantId, studentId);
        Branch branch = guard.requireBranch(tenantId, request.branchId());
        if (assignments.existsActive(tenantId, studentId, branch.id())) {
            throw com.gymsaas.shared.application.ApiException.conflict("BRANCH_ALREADY_ASSIGNED",
                    "Aluno já possui vínculo ativo com esta unidade.");
        }

        List<StudentBranchAssignment> active = assignments.findActive(tenantId, studentId);
        boolean primary = Boolean.TRUE.equals(request.primary()) || active.isEmpty();
        if (primary) {
            assignments.clearPrimary(tenantId, studentId);
        }

        Instant now = Instant.now();
        StudentBranchAssignment saved = assignments.save(new StudentBranchAssignment(
                UuidV7.generate(), tenantId, studentId, branch.id(), primary, now, null, null,
                TenantContext.userId(), now, now));

        audit.recordForTenant(tenantId, "STUDENT_BRANCH_ASSIGNED", "student", studentId, "MEDIUM",
                null, Map.of("branchId", branch.id().toString(), "primary", primary), Map.of());

        return new StudentDtos.BranchAssignmentView(saved.id(), branch.id(), branch.name(),
                saved.primary(), saved.startedAt(), saved.endedAt(), saved.transferReason());
    }
}
