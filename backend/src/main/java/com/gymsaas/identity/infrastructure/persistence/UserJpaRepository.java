package com.gymsaas.identity.infrastructure.persistence;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

public interface UserJpaRepository extends JpaRepository<UserJpaEntity, UUID> {

    @Query("select u from UserJpaEntity u where lower(u.email) = lower(:email) and u.status <> 'INACTIVE'")
    Optional<UserJpaEntity> findByEmailIgnoreCase(@Param("email") String email);

    @Query("select u from UserJpaEntity u where lower(u.email) = lower(:email)")
    Optional<UserJpaEntity> findByEmailIncludingInactive(@Param("email") String email);

    boolean existsByEmailIgnoreCase(String email);

    @Query(value = """
            select new com.gymsaas.identity.infrastructure.persistence.TenantMemberProjection(
                u.id, u.email, u.fullName, u.status, m.id, m.defaultMembership)
            from UserJpaEntity u, com.gymsaas.tenant.infrastructure.persistence.UserTenantJpaEntity m
            where u.id = m.userId and m.tenantId = :tenantId and m.status = 'ACTIVE'
              and (:status = '' or u.status = :status)
              and (:search = ''
                   or lower(u.fullName) like lower(concat('%', :search, '%'))
                   or lower(u.email) like lower(concat('%', :search, '%')))
            order by u.fullName
            """,
            countQuery = """
            select count(u)
            from UserJpaEntity u, com.gymsaas.tenant.infrastructure.persistence.UserTenantJpaEntity m
            where u.id = m.userId and m.tenantId = :tenantId and m.status = 'ACTIVE'
              and (:status = '' or u.status = :status)
              and (:search = ''
                   or lower(u.fullName) like lower(concat('%', :search, '%'))
                   or lower(u.email) like lower(concat('%', :search, '%')))
            """)
    Page<TenantMemberProjection> findMembersByTenantId(@Param("tenantId") UUID tenantId,
                                                       @Param("status") String status,
                                                       @Param("search") String search,
                                                       Pageable pageable);
}
