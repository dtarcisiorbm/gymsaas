package com.gymsaas.rbac.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface RoleJpaRepository extends JpaRepository<RoleJpaEntity, UUID> {

    Optional<RoleJpaEntity> findByCodeAndTenantIdIsNull(String code);

    @Query("""
            select distinct r from RoleJpaEntity r
            left join fetch r.permissions
            where (r.tenantId is null and r.code <> 'SAAS_ADMIN') or r.tenantId = :tenantId
            order by r.code
            """)
    List<RoleJpaEntity> findAssignableForTenant(@Param("tenantId") UUID tenantId);

    @Query("""
            select distinct r from RoleJpaEntity r
            left join fetch r.permissions
            where r.id in :ids
            """)
    List<RoleJpaEntity> findWithPermissionsByIdIn(@Param("ids") Collection<UUID> ids);

    @Query("""
            select distinct p.code from RoleJpaEntity r
            join r.permissions p
            where r.id in :roleIds
            """)
    List<String> findPermissionCodesByRoleIds(@Param("roleIds") Collection<UUID> roleIds);

    @Query("""
            select distinct p.code from RoleJpaEntity r
            join r.permissions p
            where r.code = :code and r.tenantId is null
            """)
    List<String> findPermissionCodesByRoleCode(@Param("code") String code);
}
