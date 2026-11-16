package com.gymsaas.membership.application.usecase;

import com.gymsaas.branch.domain.Branch;
import com.gymsaas.branch.domain.BranchRepository;
import com.gymsaas.membership.application.MembershipAssembler;
import com.gymsaas.membership.application.MembershipDtos;
import com.gymsaas.membership.application.MembershipGuard;
import com.gymsaas.membership.domain.Membership;
import com.gymsaas.membership.domain.MembershipRepository;
import com.gymsaas.membership.domain.MembershipSearchCriteria;
import com.gymsaas.membership.domain.MembershipStatus;
import com.gymsaas.plan.domain.Plan;
import com.gymsaas.plan.domain.PlanRepository;
import com.gymsaas.shared.domain.PageResult;
import com.gymsaas.student.domain.Student;
import com.gymsaas.student.domain.StudentRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class ListMembershipsUseCase {

    private static final Set<String> ALLOWED_SORTS =
            Set.of("createdAt", "startDate", "endDate", "registrationNumber", "status");

    private final MembershipGuard guard;
    private final MembershipRepository memberships;
    private final StudentRepository students;
    private final PlanRepository plans;
    private final BranchRepository branches;

    public ListMembershipsUseCase(MembershipGuard guard, MembershipRepository memberships, StudentRepository students,
                                  PlanRepository plans, BranchRepository branches) {
        this.guard = guard;
        this.memberships = memberships;
        this.students = students;
        this.plans = plans;
        this.branches = branches;
    }

    @Transactional(readOnly = true)
    public PageResult<MembershipDtos.MembershipSummary> execute(UUID tenantId, MembershipDtos.MembershipFilter filter) {
        int page = Math.max(0, filter.page());
        int size = filter.size() <= 0 ? 20 : Math.min(filter.size(), 100);
        String sort = filter.sort() != null && ALLOWED_SORTS.contains(filter.sort().trim())
                ? filter.sort().trim() : "createdAt";
        String direction = "asc".equalsIgnoreCase(filter.direction()) ? "asc" : "desc";
        MembershipStatus status = filter.status() == null || filter.status().isBlank()
                ? null : guard.parseStatus(filter.status());

        PageResult<Membership> result = memberships.search(tenantId, new MembershipSearchCriteria(
                MembershipGuard.trimToNull(filter.search()), filter.studentId(), filter.planId(), filter.branchId(),
                status, filter.endingFrom(), filter.endingTo(), page, size, sort, direction));

        List<Membership> content = result.content();
        Map<UUID, Student> studentMap = students.findAllByIds(tenantId,
                        content.stream().map(Membership::studentId).distinct().toList()).stream()
                .collect(Collectors.toMap(Student::id, Function.identity()));
        Map<UUID, Plan> planMap = plans.findAllByIds(tenantId,
                        content.stream().map(Membership::planId).distinct().toList()).stream()
                .collect(Collectors.toMap(Plan::id, Function.identity()));
        Map<UUID, Branch> branchMap = branches.findAllByIds(tenantId,
                        content.stream().map(Membership::branchId).distinct().toList()).stream()
                .collect(Collectors.toMap(Branch::id, Function.identity()));

        return PageResult.of(content.stream()
                .map(membership -> MembershipAssembler.summary(membership, studentMap.get(membership.studentId()),
                        planMap.get(membership.planId()), branchMap.get(membership.branchId())))
                .toList(), result.totalElements(), page, size);
    }
}
