package com.gymsaas.membership.application;

import com.gymsaas.branch.domain.BranchRepository;
import com.gymsaas.membership.domain.Membership;
import com.gymsaas.membership.domain.MembershipFreezeRepository;
import com.gymsaas.membership.domain.MembershipRepository;
import com.gymsaas.plan.domain.PlanRepository;
import com.gymsaas.student.domain.StudentRepository;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
public class MembershipDetailLoader {

    private final MembershipRepository memberships;
    private final MembershipFreezeRepository freezes;
    private final StudentRepository students;
    private final PlanRepository plans;
    private final BranchRepository branches;

    public MembershipDetailLoader(MembershipRepository memberships, MembershipFreezeRepository freezes,
                                  StudentRepository students, PlanRepository plans, BranchRepository branches) {
        this.memberships = memberships;
        this.freezes = freezes;
        this.students = students;
        this.plans = plans;
        this.branches = branches;
    }

    public MembershipDtos.MembershipDetail load(UUID tenantId, Membership membership) {
        UUID renewedById = memberships.findLiveRenewalOf(tenantId, membership.id()).map(Membership::id).orElse(null);
        return MembershipAssembler.detail(membership,
                students.findById(tenantId, membership.studentId()).orElse(null),
                plans.findById(tenantId, membership.planId()).orElse(null),
                branches.findById(tenantId, membership.branchId()).orElse(null),
                freezes.findByMembership(tenantId, membership.id()),
                renewedById);
    }
}
