package com.gymsaas.membership.application.usecase;

import com.gymsaas.membership.application.MembershipDetailLoader;
import com.gymsaas.membership.application.MembershipDraftFactory;
import com.gymsaas.membership.application.MembershipDtos;
import com.gymsaas.membership.application.MembershipGuard;
import com.gymsaas.membership.application.MembershipJournal;
import com.gymsaas.membership.domain.Membership;
import com.gymsaas.membership.domain.MembershipEventType;
import com.gymsaas.membership.domain.MembershipNumberGenerator;
import com.gymsaas.membership.domain.MembershipRepository;
import com.gymsaas.membership.domain.MembershipStatus;
import com.gymsaas.shared.application.ApiException;
import com.gymsaas.tenant.application.TenantCalendar;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

/**
 * Renewal creates a NEW membership linked to the previous one (decision D4):
 * price effective on the renewal start date, discount not inherited, previous
 * membership untouched (only a RENEWED history entry is added).
 */
@Service
public class RenewMembershipUseCase {

    private final MembershipGuard guard;
    private final MembershipDraftFactory drafts;
    private final MembershipRepository memberships;
    private final MembershipNumberGenerator numbers;
    private final MembershipJournal journal;
    private final MembershipDetailLoader detailLoader;
    private final TenantCalendar calendar;

    public RenewMembershipUseCase(MembershipGuard guard, MembershipDraftFactory drafts,
                                  MembershipRepository memberships, MembershipNumberGenerator numbers,
                                  MembershipJournal journal, MembershipDetailLoader detailLoader,
                                  TenantCalendar calendar) {
        this.guard = guard;
        this.drafts = drafts;
        this.memberships = memberships;
        this.numbers = numbers;
        this.journal = journal;
        this.detailLoader = detailLoader;
        this.calendar = calendar;
    }

    @Transactional
    public MembershipDtos.MembershipDetail execute(UUID tenantId, UUID membershipId,
                                                   MembershipDtos.RenewMembershipRequest request,
                                                   boolean canApplyDiscount) {
        Membership previous = guard.requireMembership(tenantId, membershipId);
        if (previous.status() != MembershipStatus.ACTIVE && previous.status() != MembershipStatus.EXPIRED) {
            throw ApiException.conflict("RENEWAL_NOT_ALLOWED",
                    "Somente matrículas ativas ou expiradas podem ser renovadas.");
        }
        if (memberships.findLiveRenewalOf(tenantId, membershipId).isPresent()) {
            throw ApiException.conflict("ALREADY_RENEWED", "Esta matrícula já possui renovação.");
        }
        MembershipDraftFactory.Draft draft = drafts.forRenewal(tenantId, previous, request.startDate(),
                request.branchId(), request.discountAmount(), request.discountReason(), canApplyDiscount);

        Instant now = calendar.now();
        Membership renewal = memberships.save(CreateMembershipUseCase.newMembership(tenantId, draft, previous.id(),
                MembershipGuard.trimToNull(request.notes()), numbers.next(tenantId, draft.today().getYear()), now));

        journal.record(renewal, MembershipEventType.CREATED, null, renewal.status(), null, Map.of(
                "renewedFromId", previous.id().toString(),
                "planPriceId", renewal.planPriceId().toString(),
                "finalAmount", renewal.conditions().finalAmount().toPlainString(),
                "termSequence", renewal.termSequence()), now, "MEDIUM");
        journal.record(previous, MembershipEventType.RENEWED, previous.status(), previous.status(), null, Map.of(
                "renewalId", renewal.id().toString(),
                "renewalRegistrationNumber", renewal.registrationNumber()), now, "MEDIUM");
        return detailLoader.load(tenantId, renewal);
    }
}
