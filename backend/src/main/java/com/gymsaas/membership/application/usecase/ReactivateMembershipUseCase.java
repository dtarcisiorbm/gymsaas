package com.gymsaas.membership.application.usecase;

import com.gymsaas.membership.application.MembershipDetailLoader;
import com.gymsaas.membership.application.MembershipDtos;
import com.gymsaas.membership.application.MembershipGuard;
import com.gymsaas.membership.application.MembershipJournal;
import com.gymsaas.membership.domain.Membership;
import com.gymsaas.membership.domain.MembershipEventType;
import com.gymsaas.membership.domain.MembershipRepository;
import com.gymsaas.membership.domain.MembershipStatus;
import com.gymsaas.shared.application.ApiException;
import com.gymsaas.tenant.application.TenantCalendar;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

/** Reactivates a SUSPENDED membership (frozen memberships use unfreeze). */
@Service
public class ReactivateMembershipUseCase {

    private final MembershipGuard guard;
    private final MembershipRepository memberships;
    private final MembershipJournal journal;
    private final MembershipDetailLoader detailLoader;
    private final TenantCalendar calendar;

    public ReactivateMembershipUseCase(MembershipGuard guard, MembershipRepository memberships,
                                       MembershipJournal journal, MembershipDetailLoader detailLoader,
                                       TenantCalendar calendar) {
        this.guard = guard;
        this.memberships = memberships;
        this.journal = journal;
        this.detailLoader = detailLoader;
        this.calendar = calendar;
    }

    @Transactional
    public MembershipDtos.MembershipDetail execute(UUID tenantId, UUID membershipId,
                                                   MembershipDtos.ReasonRequest request) {
        Membership current = guard.requireMembership(tenantId, membershipId);
        if (current.status() != MembershipStatus.SUSPENDED) {
            throw ApiException.conflict("INVALID_STATUS_TRANSITION", "Somente matrículas suspensas podem ser reativadas.");
        }
        if (current.endDate().isBefore(calendar.today(tenantId))) {
            throw ApiException.conflict("MEMBERSHIP_ENDED", "A vigência terminou; renove a matrícula.");
        }
        Instant now = calendar.now();
        Membership saved = memberships.save(current.withStatus(MembershipStatus.ACTIVE, now));
        journal.record(saved, MembershipEventType.REACTIVATED, current.status(), saved.status(),
                request.reason().trim(), Map.of(), now, "MEDIUM");
        return detailLoader.load(tenantId, saved);
    }
}
