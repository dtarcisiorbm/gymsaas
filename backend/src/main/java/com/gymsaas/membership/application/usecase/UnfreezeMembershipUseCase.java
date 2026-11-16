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
import com.gymsaas.shared.application.ApiException;
import com.gymsaas.shared.infrastructure.tenant.TenantContext;
import com.gymsaas.tenant.application.TenantCalendar;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;
import java.util.Map;
import java.util.UUID;

/**
 * Early reactivation of a frozen membership: the member resumes today, the
 * unused frozen days are given back (end date reduced accordingly).
 */
@Service
public class UnfreezeMembershipUseCase {

    private final MembershipGuard guard;
    private final MembershipRepository memberships;
    private final MembershipFreezeRepository freezes;
    private final MembershipJournal journal;
    private final MembershipDetailLoader detailLoader;
    private final TenantCalendar calendar;

    public UnfreezeMembershipUseCase(MembershipGuard guard, MembershipRepository memberships,
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
                                                   MembershipDtos.OptionalReasonRequest request) {
        Membership current = guard.requireMembership(tenantId, membershipId);
        if (current.status() != MembershipStatus.FROZEN) {
            throw ApiException.conflict("INVALID_STATUS_TRANSITION", "A matrícula não está congelada.");
        }
        MembershipFreeze active = freezes.findActive(tenantId, membershipId)
                .orElseThrow(() -> ApiException.conflict("FREEZE_NOT_FOUND", "Congelamento ativo não encontrado."));

        LocalDate today = calendar.today(tenantId);
        Instant now = calendar.now();
        MembershipFreeze ended = freezes.save(active.endedOn(today, TenantContext.userId(), now));
        int unusedDays = ended.plannedDays() - ended.effectiveDays();
        Membership saved = memberships.save(current.withEndDate(current.endDate().minusDays(unusedDays),
                MembershipStatus.ACTIVE, now));

        String reason = request == null ? null : MembershipGuard.trimToNull(request.reason());
        journal.record(saved, MembershipEventType.UNFROZEN, current.status(), saved.status(), reason, Map.of(
                "freezeId", ended.id().toString(),
                "effectiveDays", ended.effectiveDays(),
                "returnedDays", unusedDays,
                "newEndDate", saved.endDate().toString()), now, "MEDIUM");
        return detailLoader.load(tenantId, saved);
    }
}
