package com.gymsaas.membership.application.usecase;

import com.gymsaas.membership.application.MembershipDetailLoader;
import com.gymsaas.membership.application.MembershipDtos;
import com.gymsaas.membership.application.MembershipGuard;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
public class FindMembershipByIdUseCase {

    private final MembershipGuard guard;
    private final MembershipDetailLoader detailLoader;

    public FindMembershipByIdUseCase(MembershipGuard guard, MembershipDetailLoader detailLoader) {
        this.guard = guard;
        this.detailLoader = detailLoader;
    }

    @Transactional(readOnly = true)
    public MembershipDtos.MembershipDetail execute(UUID tenantId, UUID membershipId) {
        return detailLoader.load(tenantId, guard.requireMembership(tenantId, membershipId));
    }
}
