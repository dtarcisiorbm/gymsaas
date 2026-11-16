package com.gymsaas.membership.infrastructure.persistence;

import com.gymsaas.membership.domain.MembershipEvent;
import com.gymsaas.membership.domain.MembershipEventType;
import com.gymsaas.membership.domain.MembershipHistoryRepository;
import com.gymsaas.membership.domain.MembershipStatus;
import org.springframework.stereotype.Repository;

import java.util.HashMap;
import java.util.List;
import java.util.UUID;

@Repository
public class MembershipHistoryRepositoryAdapter implements MembershipHistoryRepository {

    private final MembershipHistoryJpaRepository repository;

    public MembershipHistoryRepositoryAdapter(MembershipHistoryJpaRepository repository) {
        this.repository = repository;
    }

    @Override
    public void append(MembershipEvent event) {
        MembershipHistoryJpaEntity entity = new MembershipHistoryJpaEntity();
        entity.id = event.id();
        entity.tenantId = event.tenantId();
        entity.membershipId = event.membershipId();
        entity.eventType = event.type().name();
        entity.fromStatus = event.fromStatus() == null ? null : event.fromStatus().name();
        entity.toStatus = event.toStatus() == null ? null : event.toStatus().name();
        entity.reason = event.reason();
        entity.metadata = new HashMap<>(event.metadata());
        entity.changedBy = event.changedBy();
        entity.changedAt = event.changedAt();
        repository.saveAndFlush(entity);
    }

    @Override
    public List<MembershipEvent> findByMembership(UUID tenantId, UUID membershipId) {
        return repository.findByTenantIdAndMembershipIdOrderByChangedAtDescIdDesc(tenantId, membershipId).stream()
                .map(entity -> new MembershipEvent(entity.id, entity.tenantId, entity.membershipId,
                        MembershipEventType.valueOf(entity.eventType),
                        entity.fromStatus == null ? null : MembershipStatus.parse(entity.fromStatus),
                        entity.toStatus == null ? null : MembershipStatus.parse(entity.toStatus), entity.reason,
                        entity.metadata, entity.changedBy, entity.changedAt))
                .toList();
    }
}
