package com.gymsaas.membership.application;

import com.gymsaas.branch.domain.Branch;
import com.gymsaas.membership.domain.ContractedConditions;
import com.gymsaas.membership.domain.FreezeStatus;
import com.gymsaas.membership.domain.Membership;
import com.gymsaas.membership.domain.MembershipEvent;
import com.gymsaas.membership.domain.MembershipFreeze;
import com.gymsaas.plan.domain.FreezePolicy;
import com.gymsaas.plan.domain.Plan;
import com.gymsaas.student.domain.Student;

import java.util.List;
import java.util.UUID;

public final class MembershipAssembler {

    private MembershipAssembler() {
    }

    public static MembershipDtos.ConditionsView conditions(ContractedConditions conditions) {
        FreezePolicy policy = conditions.freezePolicy() == null ? FreezePolicy.unlimited() : conditions.freezePolicy();
        return new MembershipDtos.ConditionsView(conditions.durationType().name(), conditions.durationValue(),
                conditions.amount(), conditions.setupFee(), conditions.currency(), conditions.billingPeriod().name(),
                conditions.billingInterval(), conditions.discountAmount(), conditions.discountReason(),
                conditions.finalAmount(),
                new MembershipDtos.FreezePolicyView(policy.maxFreezeDays(), policy.maxFreezes(), policy.minFreezeDays()),
                conditions.snapshot());
    }

    public static MembershipDtos.FreezeView freeze(MembershipFreeze freeze) {
        return new MembershipDtos.FreezeView(freeze.id(), freeze.startDate(), freeze.plannedEndDate(),
                freeze.actualEndDate(), freeze.plannedDays(), freeze.effectiveDays(), freeze.status().name(),
                freeze.reason(), freeze.createdAt(), freeze.endedAt());
    }

    public static MembershipDtos.HistoryEntryView event(MembershipEvent event) {
        return new MembershipDtos.HistoryEntryView(event.id(), event.type().name(),
                event.fromStatus() == null ? null : event.fromStatus().name(),
                event.toStatus() == null ? null : event.toStatus().name(), event.reason(), event.metadata(),
                event.changedBy(), event.changedAt());
    }

    public static MembershipDtos.MembershipSummary summary(Membership membership, Student student, Plan plan,
                                                           Branch branch) {
        return new MembershipDtos.MembershipSummary(membership.id(), membership.registrationNumber(),
                membership.status().name(), membership.studentId(), student == null ? null : student.name(),
                student == null ? null : student.registrationNumber(), membership.planId(), planName(membership, plan),
                membership.branchId(), branch == null ? null : branch.name(), membership.startDate(),
                membership.endDate(), membership.conditions().finalAmount(), membership.conditions().currency(),
                membership.conditions().billingPeriod().name(), membership.createdAt());
    }

    public static MembershipDtos.MembershipDetail detail(Membership membership, Student student, Plan plan,
                                                         Branch branch, List<MembershipFreeze> freezes,
                                                         UUID renewedById) {
        List<MembershipFreeze> counted = freezes.stream()
                .filter(freeze -> freeze.status() != FreezeStatus.CANCELLED).toList();
        int frozenDays = counted.stream().mapToInt(MembershipFreeze::consumedDays).sum();
        return new MembershipDtos.MembershipDetail(membership.id(), membership.registrationNumber(),
                membership.status().name(), membership.studentId(), student == null ? null : student.name(),
                student == null ? null : student.registrationNumber(), membership.planId(), planName(membership, plan),
                membership.planPriceId(), membership.branchId(), branch == null ? null : branch.name(),
                membership.startDate(), membership.endDate(), membership.originalEndDate(), membership.anchorDate(),
                membership.termSequence(), conditions(membership.conditions()), membership.renewedFromId(),
                renewedById, membership.notes(), membership.cancelledAt(), membership.cancelledBy(),
                membership.cancellationReason(), counted.size(), frozenDays,
                freezes.stream().map(MembershipAssembler::freeze).toList(), membership.version(),
                membership.createdAt(), membership.updatedAt());
    }

    /** Contracted plan name comes from the snapshot; the live name is only a fallback. */
    private static String planName(Membership membership, Plan plan) {
        Object snapshotName = membership.conditions().snapshot().get("planName");
        if (snapshotName instanceof String name) {
            return name;
        }
        return plan == null ? null : plan.name();
    }
}
