package com.gymsaas.membership.application;

import com.gymsaas.branch.domain.Branch;
import com.gymsaas.branch.domain.BranchRepository;
import com.gymsaas.membership.domain.Membership;
import com.gymsaas.membership.domain.MembershipRepository;
import com.gymsaas.membership.domain.MembershipStatus;
import com.gymsaas.plan.domain.Plan;
import com.gymsaas.plan.domain.PlanRepository;
import com.gymsaas.shared.application.ApiException;
import com.gymsaas.student.domain.Student;
import com.gymsaas.student.domain.StudentRepository;
import com.gymsaas.student.domain.StudentStatus;
import org.springframework.stereotype.Component;

import java.util.EnumSet;
import java.util.Set;
import java.util.UUID;

/**
 * Every lookup is scoped by the authenticated tenant, so a student, plan or
 * branch of another tenant is simply "not found" (and composite FKs + RLS
 * enforce the same at the database level).
 */
@Component
public class MembershipGuard {

    /** Students in these statuses cannot receive new memberships or renewals. */
    private static final Set<StudentStatus> INELIGIBLE = EnumSet.of(StudentStatus.BLOCKED, StudentStatus.ARCHIVED);

    private final MembershipRepository memberships;
    private final StudentRepository students;
    private final PlanRepository plans;
    private final BranchRepository branches;

    public MembershipGuard(MembershipRepository memberships, StudentRepository students, PlanRepository plans,
                           BranchRepository branches) {
        this.memberships = memberships;
        this.students = students;
        this.plans = plans;
        this.branches = branches;
    }

    public Membership requireMembership(UUID tenantId, UUID membershipId) {
        return memberships.findById(tenantId, membershipId)
                .orElseThrow(() -> ApiException.notFound("MEMBERSHIP_NOT_FOUND", "Matrícula não encontrada."));
    }

    public Student requireEligibleStudent(UUID tenantId, UUID studentId) {
        Student student = students.findById(tenantId, studentId)
                .orElseThrow(() -> ApiException.notFound("STUDENT_NOT_FOUND", "Aluno não encontrado."));
        if (INELIGIBLE.contains(student.status())) {
            throw ApiException.conflict("STUDENT_NOT_ELIGIBLE",
                    "Aluno bloqueado ou arquivado não pode receber matrícula.");
        }
        return student;
    }

    public Student findStudent(UUID tenantId, UUID studentId) {
        return students.findById(tenantId, studentId).orElse(null);
    }

    public Plan requireSellablePlan(UUID tenantId, UUID planId) {
        Plan plan = plans.findById(tenantId, planId)
                .orElseThrow(() -> ApiException.notFound("PLAN_NOT_FOUND", "Plano não encontrado."));
        if (!plan.active()) {
            throw ApiException.conflict("PLAN_NOT_ACTIVE", "Somente planos ativos aceitam novas matrículas.");
        }
        return plan;
    }

    public Branch requireActiveBranch(UUID tenantId, UUID branchId) {
        Branch branch = branches.findById(tenantId, branchId)
                .orElseThrow(() -> ApiException.notFound("BRANCH_NOT_FOUND", "Unidade não encontrada."));
        if (!branch.active()) {
            throw ApiException.conflict("BRANCH_INACTIVE", "Unidade inativa não aceita matrículas.");
        }
        return branch;
    }

    public void requireTransition(Membership membership, MembershipStatus target, String message) {
        if (!membership.status().canTransitionTo(target)) {
            throw ApiException.conflict("INVALID_STATUS_TRANSITION", message);
        }
    }

    public void requireVersion(Membership membership, Integer expectedVersion) {
        if (expectedVersion != null && expectedVersion != membership.version()) {
            throw ApiException.conflict("CONCURRENT_UPDATE",
                    "A matrícula foi alterada por outra pessoa. Recarregue e tente novamente.");
        }
    }

    public MembershipStatus parseStatus(String raw) {
        try {
            return MembershipStatus.parse(raw);
        } catch (IllegalArgumentException exception) {
            throw ApiException.badRequest("INVALID_STATUS", "Status inválido.");
        }
    }

    public static String trimToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
