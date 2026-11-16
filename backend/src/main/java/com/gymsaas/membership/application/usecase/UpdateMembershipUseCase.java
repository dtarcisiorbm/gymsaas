package com.gymsaas.membership.application.usecase;

import com.gymsaas.membership.application.MembershipDetailLoader;
import com.gymsaas.membership.application.MembershipDtos;
import com.gymsaas.membership.application.MembershipGuard;
import com.gymsaas.membership.application.MembershipJournal;
import com.gymsaas.membership.domain.Membership;
import com.gymsaas.membership.domain.MembershipEventType;
import com.gymsaas.membership.domain.MembershipRepository;
import com.gymsaas.shared.application.ApiException;
import com.gymsaas.tenant.application.TenantCalendar;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

/**
 * Updates operational data only (branch, notes). Contracted price, discount,
 * dates and plan are immutable here and at the database level.
 */
@Service
public class UpdateMembershipUseCase {

    private final MembershipGuard guard;
    private final MembershipRepository memberships;
    private final MembershipJournal journal;
    private final MembershipDetailLoader detailLoader;
    private final TenantCalendar calendar;

    public UpdateMembershipUseCase(MembershipGuard guard, MembershipRepository memberships, MembershipJournal journal,
                                   MembershipDetailLoader detailLoader, TenantCalendar calendar) {
        this.guard = guard;
        this.memberships = memberships;
        this.journal = journal;
        this.detailLoader = detailLoader;
        this.calendar = calendar;
    }

    @Transactional
    public MembershipDtos.MembershipDetail execute(UUID tenantId, UUID membershipId,
                                                   MembershipDtos.UpdateMembershipRequest request) {
        Membership current = guard.requireMembership(tenantId, membershipId);
        guard.requireVersion(current, request.version());
        boolean branchChanged = !current.branchId().equals(request.branchId());
        if (branchChanged) {
            if (!current.status().open()) {
                throw ApiException.conflict("MEMBERSHIP_CLOSED", "Matrícula encerrada não pode trocar de unidade.");
            }
            guard.requireActiveBranch(tenantId, request.branchId());
        }
        String notes = MembershipGuard.trimToNull(request.notes());
        Instant now = calendar.now();
        Membership saved = memberships.save(current.withOperational(request.branchId(), notes, now));

        Map<String, Object> metadata = new HashMap<>();
        if (branchChanged) {
            metadata.put("fromBranchId", current.branchId().toString());
            metadata.put("toBranchId", saved.branchId().toString());
        }
        metadata.put("notesChanged", !Objects.equals(current.notes(), notes));
        journal.record(saved, MembershipEventType.UPDATED, current.status(), saved.status(), null, metadata, now,
                "LOW");
        return detailLoader.load(tenantId, saved);
    }
}
