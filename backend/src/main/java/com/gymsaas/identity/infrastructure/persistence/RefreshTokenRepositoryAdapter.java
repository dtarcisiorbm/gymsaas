package com.gymsaas.identity.infrastructure.persistence;

import com.gymsaas.identity.domain.RefreshToken;
import com.gymsaas.identity.domain.RefreshTokenRepository;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

@Repository
public class RefreshTokenRepositoryAdapter implements RefreshTokenRepository {

    private final RefreshTokenJpaRepository repository;

    public RefreshTokenRepositoryAdapter(RefreshTokenJpaRepository repository) {
        this.repository = repository;
    }

    @Override
    public Optional<RefreshToken> findByTokenHash(String tokenHash) {
        return repository.findByTokenHash(tokenHash).map(this::toDomain);
    }

    @Override
    public RefreshToken save(RefreshToken token) {
        RefreshTokenJpaEntity entity = new RefreshTokenJpaEntity(token.id(), token.userId(),
                token.tenantId(), token.familyId(), token.tokenHash(), token.issuedAt(),
                token.expiresAt(), token.revokedAt(), token.revokeReason(), token.replacedById());
        return toDomain(repository.save(entity));
    }

    @Override
    public int revokeFamily(UUID familyId, String reason) {
        return repository.revokeFamily(familyId, reason, Instant.now());
    }

    @Override
    public void updateTenantForFamily(UUID familyId, UUID tenantId) {
        repository.updateTenantForFamily(familyId, tenantId);
    }

    private RefreshToken toDomain(RefreshTokenJpaEntity entity) {
        return new RefreshToken(entity.getId(), entity.getUserId(), entity.getTenantId(),
                entity.getFamilyId(), entity.getTokenHash(), entity.getIssuedAt(),
                entity.getExpiresAt(), entity.getRevokedAt(), entity.getRevokeReason(),
                entity.getReplacedById());
    }
}
