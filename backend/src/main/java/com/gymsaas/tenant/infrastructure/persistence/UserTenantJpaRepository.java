package com.gymsaas.tenant.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface UserTenantJpaRepository extends JpaRepository<UserTenantJpaEntity, UUID> {

    Optional<UserTenantJpaEntity> findByUserIdAndTenantId(UUID userId, UUID tenantId);

    @Query("""
            select new com.gymsaas.tenant.infrastructure.persistence.MembershipProjection(
                m.id, m.userId, m.tenantId, t.name, t.status, m.defaultMembership, m.status)
            from UserTenantJpaEntity m, TenantJpaEntity t
            where m.tenantId = t.id and m.userId = :userId and m.status = 'ACTIVE'
            order by t.name
            """)
    List<MembershipProjection> findActiveByUserId(@Param("userId") UUID userId);

    @Query("""
            select new com.gymsaas.tenant.infrastructure.persistence.MembershipProjection(
                m.id, m.userId, m.tenantId, t.name, t.status, m.defaultMembership, m.status)
            from UserTenantJpaEntity m, TenantJpaEntity t
            where m.tenantId = t.id and m.userId = :userId and m.tenantId = :tenantId
              and m.status = 'ACTIVE'
            """)
    Optional<MembershipProjection> findActiveByUserAndTenant(@Param("userId") UUID userId,
                                                             @Param("tenantId") UUID tenantId);
}
