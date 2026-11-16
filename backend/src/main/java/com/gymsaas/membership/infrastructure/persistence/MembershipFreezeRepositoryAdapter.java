package com.gymsaas.membership.infrastructure.persistence;

import com.gymsaas.membership.domain.FreezeStatus;
import com.gymsaas.membership.domain.MembershipFreeze;
import com.gymsaas.membership.domain.MembershipFreezeRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public class MembershipFreezeRepositoryAdapter implements MembershipFreezeRepository {

    private final MembershipFreezeJpaRepository repository;

    public MembershipFreezeRepositoryAdapter(MembershipFreezeJpaRepository repository) {
        this.repository = repository;
    }

    @Override
    public List<MembershipFreeze> findByMembership(UUID tenantId, UUID membershipId) {
        return repository.findByTenantIdAndMembershipIdOrderByStartDateDescCreatedAtDesc(tenantId, membershipId)
                .stream().map(MembershipFreezeRepositoryAdapter::toDomain).toList();
    }

    @Override
    public Optional<MembershipFreeze> findActive(UUID tenantId, UUID membershipId) {
        return repository.findFirstByTenantIdAndMembershipIdAndStatus(tenantId, membershipId,
                FreezeStatus.ACTIVE.name()).map(MembershipFreezeRepositoryAdapter::toDomain);
    }

    @Override
    public List<MembershipFreeze> findActiveEndingBefore(UUID tenantId, LocalDate date) {
        return repository.findByTenantIdAndStatusAndPlannedEndDateLessThan(tenantId, FreezeStatus.ACTIVE.name(), date)
                .stream().map(MembershipFreezeRepositoryAdapter::toDomain).toList();
    }

    @Override
    public MembershipFreeze save(MembershipFreeze freeze) {
        MembershipFreezeJpaEntity entity = repository.findById(freeze.id()).orElse(null);
        if (entity == null) {
            entity = new MembershipFreezeJpaEntity();
            entity.id = freeze.id();
            entity.tenantId = freeze.tenantId();
            entity.membershipId = freeze.membershipId();
            entity.startDate = freeze.startDate();
            entity.plannedEndDate = freeze.plannedEndDate();
            entity.plannedDays = freeze.plannedDays();
            entity.reason = freeze.reason();
            entity.createdBy = freeze.createdBy();
            entity.createdAt = freeze.createdAt();
        }
        entity.actualEndDate = freeze.actualEndDate();
        entity.effectiveDays = freeze.effectiveDays();
        entity.status = freeze.status().name();
        entity.endedBy = freeze.endedBy();
        entity.endedAt = freeze.endedAt();
        return toDomain(repository.saveAndFlush(entity));
    }

    static MembershipFreeze toDomain(MembershipFreezeJpaEntity entity) {
        return new MembershipFreeze(entity.id, entity.tenantId, entity.membershipId, entity.startDate,
                entity.plannedEndDate, entity.actualEndDate, entity.plannedDays, entity.effectiveDays,
                FreezeStatus.valueOf(entity.status), entity.reason, entity.createdBy, entity.endedBy,
                entity.createdAt, entity.endedAt);
    }
}
