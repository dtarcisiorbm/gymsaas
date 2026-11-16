package com.gymsaas.identity.domain;

import com.gymsaas.shared.domain.PageResult;

import java.util.Optional;
import java.util.UUID;

public interface UserRepository {

    Optional<User> findByEmailIgnoreCase(String email);

    Optional<User> findByEmailIncludingInactive(String email);

    Optional<User> findById(UUID id);

    User save(User user);

    boolean existsByEmail(String email);

    PageResult<TenantMember> findMembersByTenantId(UUID tenantId, String status, String search,
                                                   int page, int size);
}
