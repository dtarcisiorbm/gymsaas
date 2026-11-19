package com.gymsaas.billing.application;

import com.gymsaas.billing.domain.Charge;
import com.gymsaas.billing.domain.Payment;
import com.gymsaas.branch.domain.Branch;
import com.gymsaas.membership.domain.Membership;
import com.gymsaas.student.domain.Student;

import java.time.LocalDate;

public final class BillingAssembler {

    private BillingAssembler() {
    }

    public static BillingDtos.ChargeView charge(Charge charge, LocalDate today, Student student, Membership membership,
                                                Branch branch) {
        return new BillingDtos.ChargeView(charge.id(), charge.chargeNumber(), charge.kind().name(),
                charge.status().name(), charge.overdue(today), charge.studentId(),
                student == null ? null : student.name(), student == null ? null : student.registrationNumber(),
                charge.membershipId(), membership == null ? null : membership.registrationNumber(),
                charge.branchId(), branch == null ? null : branch.name(), charge.installmentNumber(),
                charge.installmentCount(), charge.description(), charge.amount(), charge.currency(), charge.dueDate(),
                charge.periodStart(), charge.periodEnd(), charge.paidAt(), charge.cancelledAt(), charge.cancelledBy(),
                charge.cancellationReason(), charge.notes(), charge.createdBy(), charge.createdAt());
    }

    public static BillingDtos.PaymentView payment(Payment payment) {
        return new BillingDtos.PaymentView(payment.id(), payment.paymentNumber(), payment.chargeId(),
                payment.amount(), payment.currency(), payment.method().name(), payment.paidAt(), payment.reference(),
                payment.notes(), payment.status().name(), payment.receivedBy(), payment.refundedAt(),
                payment.refundedBy(), payment.refundReason(), payment.createdAt());
    }
}
