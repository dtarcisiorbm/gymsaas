package com.gymsaas.plan.infrastructure.persistence;

import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface PlanJpaRepository extends JpaRepository<PlanJpaEntity, UUID> {

    Optional<PlanJpaEntity> findByIdAndTenantId(UUID id, UUID tenantId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select p from PlanJpaEntity p where p.id = :id and p.tenantId = :tenantId")
    Optional<PlanJpaEntity> findForUpdate(@Param("tenantId") UUID tenantId, @Param("id") UUID id);

    List<PlanJpaEntity> findByTenantIdAndIdIn(UUID tenantId, Collection<UUID> ids);

    boolean existsByTenantIdAndCode(UUID tenantId, String code);

    boolean existsByTenantIdAndCodeAndIdNot(UUID tenantId, String code, UUID id);

    @Query(value = """
            select p from PlanJpaEntity p
            where p.tenantId = :tenantId
              and (:status = '' or p.status = :status)
              and (:search = ''
                   or lower(p.name) like lower(concat('%', :search, '%'))
                   or lower(p.code) like lower(concat('%', :search, '%')))
            """,
            countQuery = """
            select count(p) from PlanJpaEntity p
            where p.tenantId = :tenantId
              and (:status = '' or p.status = :status)
              and (:search = ''
                   or lower(p.name) like lower(concat('%', :search, '%'))
                   or lower(p.code) like lower(concat('%', :search, '%')))
            """)
    Page<PlanJpaEntity> search(@Param("tenantId") UUID tenantId, @Param("status") String status,
                               @Param("search") String search, Pageable pageable);
}
