package com.gymsaas.identity.infrastructure.persistence;

import com.gymsaas.identity.domain.TenantMember;
import com.gymsaas.identity.domain.User;
import com.gymsaas.identity.domain.UserRepository;
import com.gymsaas.shared.domain.PageResult;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public class UserRepositoryAdapter implements UserRepository {

    private final UserJpaRepository repository;

    public UserRepositoryAdapter(UserJpaRepository repository) {
        this.repository = repository;
    }

    @Override
    public Optional<User> findByEmailIgnoreCase(String email) {
        return repository.findByEmailIgnoreCase(email).map(this::toDomain);
    }

    @Override
    public Optional<User> findByEmailIncludingInactive(String email) {
        return repository.findByEmailIncludingInactive(email).map(this::toDomain);
    }

    @Override
    public Optional<User> findById(UUID id) {
        return repository.findById(id).map(this::toDomain);
    }

    @Override
    public User save(User user) {
        UserJpaEntity entity = repository.findById(user.id())
                .orElseGet(() -> new UserJpaEntity(user.id(), user.email(), user.passwordHash(),
                        user.fullName(), user.phone(), user.status(), user.mfaEnabled(),
                        user.platformAdmin(), user.lastLoginAt(), user.createdAt()));
        entity.setLastLoginAt(user.lastLoginAt());
        entity.setFullName(user.fullName());
        entity.setPhone(user.phone());
        entity.setStatus(user.status());
        return toDomain(repository.save(entity));
    }

    @Override
    public boolean existsByEmail(String email) {
        return repository.existsByEmailIgnoreCase(email);
    }

    @Override
    public PageResult<TenantMember> findMembersByTenantId(UUID tenantId, String status, String search,
                                                          int page, int size) {
        Page<TenantMemberProjection> result = repository.findMembersByTenantId(tenantId,
                emptyIfBlank(status), emptyIfBlank(search), PageRequest.of(page, size));
        return PageResult.of(result.getContent().stream().map(TenantMemberProjection::toDomain).toList(),
                result.getTotalElements(), page, size);
    }

    private String emptyIfBlank(String value) {
        return value == null ? "" : value.trim();
    }

    private User toDomain(UserJpaEntity entity) {
        return new User(entity.getId(), entity.getEmail(), entity.getPasswordHash(),
                entity.getFullName(), entity.getPhone(), entity.getStatus(), entity.isMfaEnabled(),
                entity.isPlatformAdmin(), entity.getLastLoginAt(), entity.getCreatedAt());
    }
}
