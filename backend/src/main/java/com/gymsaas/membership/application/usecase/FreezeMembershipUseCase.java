package com.gymsaas.membership.application.usecase;

import com.gymsaas.membership.application.MembershipDetailLoader;
import com.gymsaas.membership.application.MembershipDtos;
import com.gymsaas.membership.application.MembershipGuard;
import com.gymsaas.membership.application.MembershipJournal;
import com.gymsaas.membership.domain.FreezeStatus;
import com.gymsaas.membership.domain.Membership;
import com.gymsaas.membership.domain.MembershipEventType;
import com.gymsaas.membership.domain.MembershipFreeze;
import com.gymsaas.membership.domain.MembershipFreezeRepository;
import com.gymsaas.membership.domain.MembershipRepository;
import com.gymsaas.membership.domain.MembershipStatus;
import com.gymsaas.plan.domain.FreezePolicy;
import com.gymsaas.shared.application.ApiException;
import com.gymsaas.shared.domain.UuidV7;
import com.gymsaas.shared.infrastructure.tenant.TenantContext;
import com.gymsaas.tenant.application.TenantCalendar;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Freezes an ACTIVE membership from today (tenant time zone) until the
 * requested end date, inclusive. The membership end date is extended by the
 * planned days (decision D3). Limits come from the freeze policy snapshotted
 * at contracting time, not from the current plan.
 */
@Service
public class FreezeMembershipUseCase {

    private static final Map<String, String> MESSAGES = Map.of(
            "FREEZE_NOT_ALLOWED", "O plano contratado não permite congelamento.",
            "FREEZE_LIMIT_REACHED", "Limite de congelamentos do contrato atingido.",
            "FREEZE_TOO_SHORT", "Período de congelamento menor que o mínimo do contrato.",
            "FREEZE_DAYS_EXCEEDED", "Total de dias congelados excede o limite do contrato.",
            "INVALID_FREEZE_PERIOD", "Período de congelamento inválido.");

    private final MembershipGuard guard;
    private final MembershipRepository memberships;
    private final MembershipFreezeRepository freezes;
    private final MembershipJournal journal;
    private final MembershipDetailLoader detailLoader;
    private final TenantCalendar calendar;

    public FreezeMembershipUseCase(MembershipGuard guard, MembershipRepository memberships,
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
                                                   MembershipDtos.FreezeMembershipRequest request) {
        Membership current = guard.requireMembership(tenantId, membershipId);
        if (current.status() != MembershipStatus.ACTIVE) {
            throw ApiException.conflict("INVALID_STATUS_TRANSITION", "Somente matrículas ativas podem ser congeladas.");
        }
        LocalDate today = calendar.today(tenantId);
        if (request.endDate().isBefore(today)) {
            throw ApiException.badRequest("INVALID_FREEZE_PERIOD", "O fim do congelamento não pode ser anterior a hoje.");
        }
        if (memberships.findLiveRenewalOf(tenantId, membershipId).isPresent()) {
            throw ApiException.conflict("FREEZE_BLOCKED_BY_RENEWAL",
                    "Matrícula com renovação programada não pode ser congelada.");
        }

        int plannedDays = MembershipFreeze.plannedDays(today, request.endDate());
        List<MembershipFreeze> counted = freezes.findByMembership(tenantId, membershipId).stream()
                .filter(freeze -> freeze.status() != FreezeStatus.CANCELLED).toList();
        int frozenDaysSoFar = counted.stream().mapToInt(MembershipFreeze::consumedDays).sum();
        FreezePolicy policy = current.conditions().freezePolicy() == null
                ? FreezePolicy.unlimited() : current.conditions().freezePolicy();
        String violation = policy.freezeViolation(counted.size(), frozenDaysSoFar, plannedDays);
        if (violation != null) {
            throw ApiException.conflict(violation, MESSAGES.getOrDefault(violation, "Congelamento não permitido."));
        }

        Instant now = calendar.now();
        String reason = request.reason().trim();
        MembershipFreeze freeze = freezes.save(new MembershipFreeze(UuidV7.generate(), tenantId, membershipId, today,
                request.endDate(), null, plannedDays, null, FreezeStatus.ACTIVE, reason, TenantContext.userId(), null,
                now, null));
        Membership saved = memberships.save(current.withEndDate(current.endDate().plusDays(plannedDays),
                MembershipStatus.FROZEN, now));

        journal.record(saved, MembershipEventType.FROZEN, current.status(), saved.status(), reason, Map.of(
                "freezeId", freeze.id().toString(),
                "startDate", freeze.startDate().toString(),
                "plannedEndDate", freeze.plannedEndDate().toString(),
                "plannedDays", plannedDays,
                "previousEndDate", current.endDate().toString(),
                "newEndDate", saved.endDate().toString()), now, "MEDIUM");
        return detailLoader.load(tenantId, saved);
    }
}
