package com.gymsaas.billing.application.usecase;

import com.gymsaas.billing.application.BillingDtos;
import com.gymsaas.billing.application.BillingJournal;
import com.gymsaas.billing.application.BillingLoader;
import com.gymsaas.billing.domain.BillingNumberGenerator;
import com.gymsaas.billing.domain.Charge;
import com.gymsaas.billing.domain.ChargeKind;
import com.gymsaas.billing.domain.ChargeRepository;
import com.gymsaas.billing.domain.ChargeStatus;
import com.gymsaas.branch.domain.Branch;
import com.gymsaas.branch.domain.BranchRepository;
import com.gymsaas.membership.domain.Membership;
import com.gymsaas.membership.domain.MembershipRepository;
import com.gymsaas.shared.application.ApiException;
import com.gymsaas.shared.domain.Money;
import com.gymsaas.shared.domain.UuidV7;
import com.gymsaas.shared.infrastructure.tenant.TenantContext;
import com.gymsaas.student.domain.Student;
import com.gymsaas.student.domain.StudentRepository;
import com.gymsaas.student.domain.StudentStatus;
import com.gymsaas.tenant.application.TenantCalendar;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/** Decision F8: a one-off charge (product, day pass...), optionally linked to a membership of the same student. */
@Service
public class CreateManualChargeUseCase {

    private final ChargeRepository charges;
    private final BillingNumberGenerator numbers;
    private final StudentRepository students;
    private final BranchRepository branches;
    private final MembershipRepository memberships;
    private final BillingJournal journal;
    private final BillingLoader loader;
    private final TenantCalendar calendar;

    public CreateManualChargeUseCase(ChargeRepository charges, BillingNumberGenerator numbers,
                                     StudentRepository students, BranchRepository branches,
                                     MembershipRepository memberships, BillingJournal journal, BillingLoader loader,
                                     TenantCalendar calendar) {
        this.charges = charges;
        this.numbers = numbers;
        this.students = students;
        this.branches = branches;
        this.memberships = memberships;
        this.journal = journal;
        this.loader = loader;
        this.calendar = calendar;
    }

    @Transactional
    public BillingDtos.ChargeDetail execute(UUID tenantId, BillingDtos.CreateChargeRequest request) {
        Student student = students.findById(tenantId, request.studentId())
                .orElseThrow(() -> ApiException.notFound("STUDENT_NOT_FOUND", "Aluno não encontrado."));
        if (student.status() == StudentStatus.ARCHIVED) {
            throw ApiException.conflict("STUDENT_NOT_ELIGIBLE", "Aluno arquivado não pode receber cobranças.");
        }
        Branch branch = branches.findById(tenantId, request.branchId())
                .orElseThrow(() -> ApiException.notFound("BRANCH_NOT_FOUND", "Unidade não encontrada."));
        if (request.membershipId() != null) {
            Membership membership = memberships.findById(tenantId, request.membershipId())
                    .orElseThrow(() -> ApiException.notFound("MEMBERSHIP_NOT_FOUND", "Matrícula não encontrada."));
            if (!membership.studentId().equals(student.id())) {
                throw ApiException.badRequest("MEMBERSHIP_STUDENT_MISMATCH",
                        "A matrícula informada não pertence ao aluno.");
            }
        }
        BigDecimal amount;
        try {
            amount = Money.normalize(request.amount());
        } catch (IllegalArgumentException exception) {
            throw ApiException.badRequest("INVALID_AMOUNT", "Valor inválido.");
        }
        if (amount.signum() <= 0) {
            throw ApiException.badRequest("INVALID_AMOUNT", "O valor deve ser maior que zero.");
        }
        Instant now = calendar.now();
        Charge saved = charges.insert(new Charge(UuidV7.generate(), tenantId,
                numbers.nextCharge(tenantId, calendar.today(tenantId).getYear()), student.id(),
                request.membershipId(), branch.id(), ChargeKind.MANUAL, null, null, request.description().trim(),
                amount, Money.DEFAULT_CURRENCY, request.dueDate(), null, null, ChargeStatus.OPEN, null, null, null,
                null, BillingJournal.trimToNull(request.notes()), TenantContext.userId(), now, now));
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("chargeNumber", saved.chargeNumber());
        metadata.put("amount", saved.amount().toPlainString());
        metadata.put("dueDate", saved.dueDate().toString());
        if (saved.membershipId() != null) {
            metadata.put("membershipId", saved.membershipId().toString());
        }
        journal.record(tenantId, "CHARGE_CREATED", "charge", saved.id(), "MEDIUM", null, saved.status().name(),
                metadata);
        return loader.detail(tenantId, saved);
    }
}
