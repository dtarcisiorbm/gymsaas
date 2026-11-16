package com.gymsaas.membership.application;

import com.gymsaas.branch.domain.Branch;
import com.gymsaas.membership.domain.Membership;
import com.gymsaas.membership.domain.MembershipRepository;
import com.gymsaas.membership.domain.MembershipStatus;
import com.gymsaas.membership.domain.MembershipTerm;
import com.gymsaas.plan.domain.Plan;
import com.gymsaas.shared.application.ApiException;
import com.gymsaas.student.domain.Student;
import com.gymsaas.tenant.application.TenantCalendar;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

/**
 * Validates and prices a new membership or a renewal without persisting it.
 * Shared by quote, create and renew so the summary shown to the operator is
 * exactly what will be contracted.
 */
@Component
public class MembershipDraftFactory {

    public record Draft(Student student, Plan plan, Branch branch, LocalDate today, MembershipTerm.Period period,
                        MembershipPricing.Quote quote, MembershipStatus initialStatus) {
    }

    private final MembershipGuard guard;
    private final MembershipPricing pricing;
    private final MembershipRepository memberships;
    private final TenantCalendar calendar;

    public MembershipDraftFactory(MembershipGuard guard, MembershipPricing pricing, MembershipRepository memberships,
                                  TenantCalendar calendar) {
        this.guard = guard;
        this.pricing = pricing;
        this.memberships = memberships;
        this.calendar = calendar;
    }

    public Draft forNew(UUID tenantId, UUID studentId, UUID planId, UUID branchId, LocalDate requestedStart,
                        BigDecimal discount, String discountReason, boolean canApplyDiscount) {
        Student student = guard.requireEligibleStudent(tenantId, studentId);
        Plan plan = guard.requireSellablePlan(tenantId, planId);
        Branch branch = guard.requireActiveBranch(tenantId, branchId);
        LocalDate today = calendar.today(tenantId);
        LocalDate start = requestedStart == null ? today : requestedStart;
        MembershipTerm.Period period = MembershipTerm.first(start, plan.durationType(), plan.durationValue());
        return complete(tenantId, student, plan, branch, today, period, discount, discountReason, canApplyDiscount);
    }

    public Draft forRenewal(UUID tenantId, Membership previous, LocalDate requestedStart, UUID branchId,
                            BigDecimal discount, String discountReason, boolean canApplyDiscount) {
        Student student = guard.requireEligibleStudent(tenantId, previous.studentId());
        Plan plan = guard.requireSellablePlan(tenantId, previous.planId());
        Branch branch = guard.requireActiveBranch(tenantId, branchId == null ? previous.branchId() : branchId);
        LocalDate today = calendar.today(tenantId);
        LocalDate dayAfterEnd = previous.endDate().plusDays(1);
        LocalDate defaultStart = previous.status() == MembershipStatus.ACTIVE || dayAfterEnd.isAfter(today)
                ? dayAfterEnd : today;
        LocalDate start = requestedStart == null ? defaultStart : requestedStart;
        if (!start.isAfter(previous.endDate())) {
            throw ApiException.badRequest("RENEWAL_START_INVALID",
                    "A renovação deve começar após o término da matrícula atual.");
        }
        MembershipTerm.Period period = MembershipTerm.renewal(previous, start, plan.durationType(),
                plan.durationValue());
        return complete(tenantId, student, plan, branch, today, period, discount, discountReason, canApplyDiscount);
    }

    private Draft complete(UUID tenantId, Student student, Plan plan, Branch branch, LocalDate today,
                           MembershipTerm.Period period, BigDecimal discount, String discountReason,
                           boolean canApplyDiscount) {
        if (period.endDate().isBefore(today)) {
            throw ApiException.badRequest("MEMBERSHIP_ALREADY_ENDED",
                    "A vigência calculada já terminou; ajuste a data de início.");
        }
        MembershipPricing.Quote quote = pricing.quote(tenantId, plan, period.startDate(), discount, discountReason,
                canApplyDiscount);
        if (memberships.existsOpenOverlap(tenantId, student.id(), plan.id(), period.startDate(), period.endDate(),
                null)) {
            throw ApiException.conflict("MEMBERSHIP_OVERLAP",
                    "O aluno já possui matrícula deste plano no período informado.");
        }
        MembershipStatus status = period.startDate().isAfter(today) ? MembershipStatus.PENDING
                : MembershipStatus.ACTIVE;
        return new Draft(student, plan, branch, today, period, quote, status);
    }
}
