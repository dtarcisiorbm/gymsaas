package com.gymsaas.branch.infrastructure.persistence;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface BranchJpaRepository extends JpaRepository<BranchJpaEntity, UUID> {

    Optional<BranchJpaEntity> findByIdAndTenantId(UUID id, UUID tenantId);

    Optional<BranchJpaEntity> findByTenantIdAndCodeIgnoreCase(UUID tenantId, String code);

    List<BranchJpaEntity> findByTenantIdAndIdIn(UUID tenantId, Collection<UUID> ids);

    @Query(value = """
            select b from BranchJpaEntity b
            where b.tenantId = :tenantId
              and (:status = '' or b.status = :status)
              and (:search = ''
                   or lower(b.name) like lower(concat('%', :search, '%'))
                   or lower(b.code) like lower(concat('%', :search, '%'))
                   or lower(coalesce(b.city, '')) like lower(concat('%', :search, '%')))
            order by b.name
            """,
            countQuery = """
            select count(b) from BranchJpaEntity b
            where b.tenantId = :tenantId
              and (:status = '' or b.status = :status)
              and (:search = ''
                   or lower(b.name) like lower(concat('%', :search, '%'))
                   or lower(b.code) like lower(concat('%', :search, '%'))
                   or lower(coalesce(b.city, '')) like lower(concat('%', :search, '%')))
            """)
    Page<BranchJpaEntity> search(@Param("tenantId") UUID tenantId,
                                 @Param("status") String status,
                                 @Param("search") String search,
                                 Pageable pageable);
}
