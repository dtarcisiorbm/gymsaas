package com.gymsaas.membership.application.usecase;

import com.gymsaas.membership.application.MembershipDtos;
import com.gymsaas.membership.application.MembershipJournal;
import com.gymsaas.membership.domain.Membership;
import com.gymsaas.membership.domain.MembershipEventType;
import com.gymsaas.membership.domain.MembershipFreeze;
import com.gymsaas.membership.domain.MembershipFreezeRepository;
import com.gymsaas.membership.domain.MembershipRepository;
import com.gymsaas.membership.domain.MembershipStatus;
import com.gymsaas.tenant.application.TenantCalendar;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;
import java.util.Map;
import java.util.UUID;

/**
 * Date-driven transitions for one tenant, using the tenant's local date.
 * Idempotent: running it several times on the same day changes nothing more.
 * Order: finish expired freezes, activate pending memberships, expire ended.
 */
@Service
public class ProcessMembershipLifecycleUseCase {

    private final MembershipRepository memberships;
    private final MembershipFreezeRepository freezes;
    private final MembershipJournal journal;
    private final TenantCalendar calendar;

    public ProcessMembershipLifecycleUseCase(MembershipRepository memberships, MembershipFreezeRepository freezes,
                                             MembershipJournal journal, TenantCalendar calendar) {
        this.memberships = memberships;
        this.freezes = freezes;
        this.journal = journal;
        this.calendar = calendar;
    }

    @Transactional
    public MembershipDtos.LifecycleResult execute(UUID tenantId) {
        return execute(tenantId, calendar.today(tenantId));
    }

    @Transactional
    public MembershipDtos.LifecycleResult execute(UUID tenantId, LocalDate today) {
        Instant now = calendar.now();
        int unfrozen = 0;
        for (MembershipFreeze freeze : freezes.findActiveEndingBefore(tenantId, today)) {
            Membership membership = memberships.findById(tenantId, freeze.membershipId()).orElse(null);
            MembershipFreeze ended = freezes.save(freeze.endedOn(freeze.plannedEndDate().plusDays(1), null, now));
            if (membership != null && membership.status() == MembershipStatus.FROZEN) {
                Membership saved = memberships.save(membership.withStatus(MembershipStatus.ACTIVE, now));
                journal.record(saved, MembershipEventType.UNFROZEN, MembershipStatus.FROZEN, MembershipStatus.ACTIVE,
                        "Fim do período de congelamento", Map.of("freezeId", ended.id().toString(),
                                "effectiveDays", ended.effectiveDays(), "system", true), now, "LOW");
                unfrozen++;
            }
        }

        int activated = 0;
        for (Membership membership : memberships.findPendingStartingOnOrBefore(tenantId, today)) {
            Membership saved = memberships.save(membership.withStatus(MembershipStatus.ACTIVE, now));
            journal.record(saved, MembershipEventType.ACTIVATED, MembershipStatus.PENDING, MembershipStatus.ACTIVE,
                    "Início da vigência", Map.of("system", true), now, "LOW");
            activated++;
        }

        int expired = 0;
        for (Membership membership : memberships.findRunningEndedBefore(tenantId, today)) {
            Membership saved = memberships.save(membership.withStatus(MembershipStatus.EXPIRED, now));
            journal.record(saved, MembershipEventType.EXPIRED, membership.status(), MembershipStatus.EXPIRED,
                    "Fim da vigência", Map.of("system", true, "endDate", membership.endDate().toString()), now, "LOW");
            expired++;
        }
        return new MembershipDtos.LifecycleResult(activated, unfrozen, expired);
    }
}
