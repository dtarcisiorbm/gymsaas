package com.gymsaas.tenant.infrastructure.persistence;

import com.gymsaas.tenant.domain.Membership;
import com.gymsaas.tenant.domain.MembershipRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public class MembershipRepositoryAdapter implements MembershipRepository {

    private final UserTenantJpaRepository repository;

    public MembershipRepositoryAdapter(UserTenantJpaRepository repository) {
        this.repository = repository;
    }

    @Override
    public List<Membership> findActiveByUserId(UUID userId) {
        return repository.findActiveByUserId(userId).stream().map(this::toDomain).toList();
    }

    @Override
    public Optional<Membership> findActiveByUserAndTenant(UUID userId, UUID tenantId) {
        return repository.findActiveByUserAndTenant(userId, tenantId).map(this::toDomain);
    }

    @Override
    public Optional<Membership> findById(UUID id) {
        return repository.findById(id).map(entity -> new Membership(entity.getId(), entity.getUserId(),
                entity.getTenantId(), null, null, entity.isDefaultMembership(), entity.getStatus()));
    }

    @Override
    public Optional<Membership> findByUserAndTenantIncludingInactive(UUID userId, UUID tenantId) {
        return repository.findByUserIdAndTenantId(userId, tenantId)
                .map(entity -> new Membership(entity.getId(), entity.getUserId(), entity.getTenantId(),
                        null, null, entity.isDefaultMembership(), entity.getStatus()));
    }

    @Override
    public Membership create(UUID id, UUID userId, UUID tenantId, boolean defaultMembership) {
        UserTenantJpaEntity entity = repository.save(new UserTenantJpaEntity(
                id, userId, tenantId, defaultMembership, "ACTIVE"));
        return new Membership(entity.getId(), entity.getUserId(), entity.getTenantId(),
                null, null, entity.isDefaultMembership(), entity.getStatus());
    }

    private Membership toDomain(MembershipProjection projection) {
        return new Membership(projection.id(), projection.userId(), projection.tenantId(),
                projection.tenantName(), projection.tenantStatus(), projection.defaultMembership(),
                projection.status());
    }
}
