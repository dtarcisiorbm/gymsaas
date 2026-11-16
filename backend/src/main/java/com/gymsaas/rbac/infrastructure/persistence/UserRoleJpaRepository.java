package com.gymsaas.rbac.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface UserRoleJpaRepository extends JpaRepository<UserRoleJpaEntity, UUID> {

    List<UserRoleJpaEntity> findByUserTenantId(UUID userTenantId);

    void deleteByUserTenantId(UUID userTenantId);
}
