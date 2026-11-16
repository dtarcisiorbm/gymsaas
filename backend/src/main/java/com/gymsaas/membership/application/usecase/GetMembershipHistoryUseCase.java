package com.gymsaas.membership.application.usecase;

import com.gymsaas.membership.application.MembershipAssembler;
import com.gymsaas.membership.application.MembershipDtos;
import com.gymsaas.membership.application.MembershipGuard;
import com.gymsaas.membership.domain.MembershipHistoryRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
public class GetMembershipHistoryUseCase {

    private final MembershipGuard guard;
    private final MembershipHistoryRepository history;

    public GetMembershipHistoryUseCase(MembershipGuard guard, MembershipHistoryRepository history) {
        this.guard = guard;
        this.history = history;
    }

    @Transactional(readOnly = true)
    public List<MembershipDtos.HistoryEntryView> execute(UUID tenantId, UUID membershipId) {
        guard.requireMembership(tenantId, membershipId);
        return history.findByMembership(tenantId, membershipId).stream().map(MembershipAssembler::event).toList();
    }
}
