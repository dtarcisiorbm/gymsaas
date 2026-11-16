package com.gymsaas.membership.application.usecase;

import com.gymsaas.membership.application.MembershipDetailLoader;
import com.gymsaas.membership.application.MembershipDtos;
import com.gymsaas.membership.application.MembershipGuard;
import com.gymsaas.membership.application.MembershipJournal;
import com.gymsaas.membership.domain.Membership;
import com.gymsaas.membership.domain.MembershipEventType;
import com.gymsaas.membership.domain.MembershipFreeze;
import com.gymsaas.membership.domain.MembershipFreezeRepository;
import com.gymsaas.membership.domain.MembershipRepository;
import com.gymsaas.membership.domain.MembershipStatus;
import com.gymsaas.shared.infrastructure.tenant.TenantContext;
import com.gymsaas.tenant.application.TenantCalendar;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * Cancels with mandatory reason, timestamp and responsible user. No financial
 * effect in this phase (refunds belong to the financial module). An active
 * freeze is closed so the freeze history stays consistent.
 */
@Service
public class CancelMembershipUseCase {

    private final MembershipGuard guard;
    private final MembershipRepository memberships;
    private final MembershipFreezeRepository freezes;
    private final MembershipJournal journal;
    private final MembershipDetailLoader detailLoader;
    private final TenantCalendar calendar;

    public CancelMembershipUseCase(MembershipGuard guard, MembershipRepository memberships,
                                   MembershipFreezeRepository freezes, MembershipJournal journal,
                                   MembershipDetailLoader detailLoader, TenantCalendar calendar) {
        this.guard = guard;
        this.memberships = memberships;
        this.freezes = freezes;
        this.journal = journal;
        this.detailLoader = detailLoader;
        this.calendar = calendar;
    }

    @Transactional
    public MembershipDtos.MembershipDetail execute(UUID tenantId, UUID membershipId,
                                                   MembershipDtos.ReasonRequest request) {
        Membership current = guard.requireMembership(tenantId, membershipId);
        guard.requireTransition(current, MembershipStatus.CANCELLED, "Matrícula já encerrada.");
        Instant now = calendar.now();
        LocalDate today = calendar.today(tenantId);
        UUID actor = TenantContext.userId();
        String reason = request.reason().trim();

        Membership toSave = current;
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("effectiveDate", today.toString());
        Optional<MembershipFreeze> activeFreeze = freezes.findActive(tenantId, membershipId);
        if (activeFreeze.isPresent()) {
            MembershipFreeze ended = freezes.save(activeFreeze.get().endedOn(today, actor, now));
            int unused = ended.plannedDays() - ended.effectiveDays();
            toSave = current.withEndDate(current.endDate().minusDays(unused), current.status(), now);
            metadata.put("closedFreezeId", ended.id().toString());
        }
        Membership saved = memberships.save(toSave.withCancellation(now, actor, reason));
        journal.record(saved, MembershipEventType.CANCELLED, current.status(), MembershipStatus.CANCELLED, reason,
                metadata, now, "HIGH");
        return detailLoader.load(tenantId, saved);
    }
}
