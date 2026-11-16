package com.gymsaas.membership.application.usecase;

import com.gymsaas.membership.application.MembershipDetailLoader;
import com.gymsaas.membership.application.MembershipDtos;
import com.gymsaas.membership.application.MembershipGuard;
import com.gymsaas.membership.application.MembershipJournal;
import com.gymsaas.membership.domain.Membership;
import com.gymsaas.membership.domain.MembershipEventType;
import com.gymsaas.membership.domain.MembershipRepository;
import com.gymsaas.membership.domain.MembershipStatus;
import com.gymsaas.tenant.application.TenantCalendar;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

/** Administrative suspension. Unlike a freeze it does NOT extend the end date. */
@Service
public class SuspendMembershipUseCase {

    private final MembershipGuard guard;
    private final MembershipRepository memberships;
    private final MembershipJournal journal;
    private final MembershipDetailLoader detailLoader;
    private final TenantCalendar calendar;

    public SuspendMembershipUseCase(MembershipGuard guard, MembershipRepository memberships, MembershipJournal journal,
                                    MembershipDetailLoader detailLoader, TenantCalendar calendar) {
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
        if (current.status() != MembershipStatus.ACTIVE) {
            guard.requireTransition(current, MembershipStatus.SUSPENDED, "Somente matrículas ativas podem ser suspensas.");
        }
        Instant now = calendar.now();
        Membership saved = memberships.save(current.withStatus(MembershipStatus.SUSPENDED, now));
        journal.record(saved, MembershipEventType.SUSPENDED, current.status(), saved.status(), request.reason().trim(),
                Map.of(), now, "HIGH");
        return detailLoader.load(tenantId, saved);
    }
}
