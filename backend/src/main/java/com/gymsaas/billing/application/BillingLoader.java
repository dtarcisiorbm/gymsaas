package com.gymsaas.billing.application;

import com.gymsaas.billing.domain.Charge;
import com.gymsaas.billing.domain.ChargeRepository;
import com.gymsaas.billing.domain.PaymentRepository;
import com.gymsaas.branch.domain.Branch;
import com.gymsaas.branch.domain.BranchRepository;
import com.gymsaas.membership.domain.Membership;
import com.gymsaas.membership.domain.MembershipRepository;
import com.gymsaas.shared.application.ApiException;
import com.gymsaas.student.domain.Student;
import com.gymsaas.student.domain.StudentRepository;
import com.gymsaas.tenant.application.TenantCalendar;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

/** Builds charge views with student/membership/branch labels resolved in batch (tenant scoped). */
@Component
public class BillingLoader {

    private final ChargeRepository charges;
    private final PaymentRepository payments;
    private final StudentRepository students;
    private final MembershipRepository memberships;
    private final BranchRepository branches;
    private final TenantCalendar calendar;

    public BillingLoader(ChargeRepository charges, PaymentRepository payments, StudentRepository students,
                         MembershipRepository memberships, BranchRepository branches, TenantCalendar calendar) {
        this.charges = charges;
        this.payments = payments;
        this.students = students;
        this.memberships = memberships;
        this.branches = branches;
        this.calendar = calendar;
    }

    public Charge requireCharge(UUID tenantId, UUID chargeId) {
        return charges.findById(tenantId, chargeId)
                .orElseThrow(() -> ApiException.notFound("CHARGE_NOT_FOUND", "Cobrança não encontrada."));
    }

    public BillingDtos.ChargeDetail detail(UUID tenantId, Charge charge) {
        BillingDtos.ChargeView view = views(tenantId, List.of(charge)).get(0);
        return new BillingDtos.ChargeDetail(view, payments.findByCharge(tenantId, charge.id()).stream()
                .map(BillingAssembler::payment).toList());
    }

    public List<BillingDtos.ChargeView> views(UUID tenantId, List<Charge> content) {
        if (content.isEmpty()) {
            return List.of();
        }
        LocalDate today = calendar.today(tenantId);
        Map<UUID, Student> studentMap = students.findAllByIds(tenantId,
                        content.stream().map(Charge::studentId).distinct().toList()).stream()
                .collect(Collectors.toMap(Student::id, Function.identity()));
        Map<UUID, Membership> membershipMap = memberships.findAllByIds(tenantId,
                        content.stream().map(Charge::membershipId).filter(Objects::nonNull).distinct().toList())
                .stream().collect(Collectors.toMap(Membership::id, Function.identity()));
        Map<UUID, Branch> branchMap = branches.findAllByIds(tenantId,
                        content.stream().map(Charge::branchId).distinct().toList()).stream()
                .collect(Collectors.toMap(Branch::id, Function.identity()));
        return content.stream().map(charge -> BillingAssembler.charge(charge, today,
                studentMap.get(charge.studentId()),
                charge.membershipId() == null ? null : membershipMap.get(charge.membershipId()),
                branchMap.get(charge.branchId()))).toList();
    }
}
