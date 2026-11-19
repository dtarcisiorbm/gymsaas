package com.gymsaas.membership.application.usecase;

import com.gymsaas.membership.application.MembershipBillingPort;
import com.gymsaas.membership.application.MembershipDetailLoader;
import com.gymsaas.membership.application.MembershipDraftFactory;
import com.gymsaas.membership.application.MembershipDtos;
import com.gymsaas.membership.application.MembershipGuard;
import com.gymsaas.membership.application.MembershipJournal;
import com.gymsaas.membership.domain.Membership;
import com.gymsaas.membership.domain.MembershipEventType;
import com.gymsaas.membership.domain.MembershipNumberGenerator;
import com.gymsaas.membership.domain.MembershipRepository;
import com.gymsaas.shared.domain.UuidV7;
import com.gymsaas.shared.infrastructure.tenant.TenantContext;
import com.gymsaas.tenant.application.TenantCalendar;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

@Service
public class CreateMembershipUseCase {

    private final MembershipDraftFactory drafts;
    private final MembershipRepository memberships;
    private final MembershipNumberGenerator numbers;
    private final MembershipJournal journal;
    private final MembershipDetailLoader detailLoader;
    private final TenantCalendar calendar;
    private final MembershipBillingPort billing;

    public CreateMembershipUseCase(MembershipDraftFactory drafts, MembershipRepository memberships,
                                   MembershipNumberGenerator numbers, MembershipJournal journal,
                                   MembershipDetailLoader detailLoader, TenantCalendar calendar,
                                   MembershipBillingPort billing) {
        this.drafts = drafts;
        this.memberships = memberships;
        this.numbers = numbers;
        this.journal = journal;
        this.detailLoader = detailLoader;
        this.calendar = calendar;
        this.billing = billing;
    }

    @Transactional
    public MembershipDtos.MembershipDetail execute(UUID tenantId, MembershipDtos.CreateMembershipRequest request,
                                                   boolean canApplyDiscount) {
        MembershipDraftFactory.Draft draft = drafts.forNew(tenantId, request.studentId(), request.planId(),
                request.branchId(), request.startDate(), request.discountAmount(), request.discountReason(),
                canApplyDiscount);
        Membership saved = memberships.save(newMembership(tenantId, draft, null,
                MembershipGuard.trimToNull(request.notes()), numbers.next(tenantId, draft.today().getYear()),
                calendar.now()));
        journal.record(saved, MembershipEventType.CREATED, null, saved.status(), null, Map.of(
                "planPriceId", saved.planPriceId().toString(),
                "finalAmount", saved.conditions().finalAmount().toPlainString(),
                "startDate", saved.startDate().toString(),
                "endDate", saved.endDate().toString()), saved.createdAt(), "MEDIUM");
        billing.onContracted(saved, false);
        return detailLoader.load(tenantId, saved);
    }

    static Membership newMembership(UUID tenantId, MembershipDraftFactory.Draft draft, UUID renewedFromId,
                                    String notes, String registrationNumber, Instant now) {
        return new Membership(UuidV7.generate(), tenantId, draft.student().id(), draft.plan().id(),
                draft.quote().price().id(), draft.branch().id(), registrationNumber, draft.initialStatus(),
                draft.period().startDate(), draft.period().endDate(), draft.period().endDate(),
                draft.period().anchorDate(), draft.period().termSequence(), draft.quote().conditions(),
                renewedFromId, notes, null, null, null, TenantContext.userId(), 0, now, now);
    }
}
