package com.gymsaas.membership.application.usecase;

import com.gymsaas.membership.application.MembershipAssembler;
import com.gymsaas.membership.application.MembershipDraftFactory;
import com.gymsaas.membership.application.MembershipDtos;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/** Contracting summary computed by the backend (nothing is persisted). */
@Service
public class QuoteMembershipUseCase {

    private final MembershipDraftFactory drafts;

    public QuoteMembershipUseCase(MembershipDraftFactory drafts) {
        this.drafts = drafts;
    }

    @Transactional(readOnly = true)
    public MembershipDtos.QuoteView execute(UUID tenantId, MembershipDtos.CreateMembershipRequest request,
                                            boolean canApplyDiscount) {
        MembershipDraftFactory.Draft draft = drafts.forNew(tenantId, request.studentId(), request.planId(),
                request.branchId(), request.startDate(), request.discountAmount(), request.discountReason(),
                canApplyDiscount);
        return new MembershipDtos.QuoteView(draft.student().id(), draft.student().name(), draft.plan().id(),
                draft.plan().name(), draft.quote().price().id(), draft.branch().id(), draft.branch().name(),
                draft.period().startDate(), draft.period().endDate(), draft.initialStatus().name(),
                MembershipAssembler.conditions(draft.quote().conditions()));
    }
}
