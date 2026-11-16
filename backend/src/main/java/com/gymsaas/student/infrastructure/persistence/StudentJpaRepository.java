package com.gymsaas.student.infrastructure.persistence;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

public interface StudentJpaRepository extends JpaRepository<StudentJpaEntity, UUID> {

    Optional<StudentJpaEntity> findByIdAndTenantIdAndDeletedAtIsNull(UUID id, UUID tenantId);

    java.util.List<StudentJpaEntity> findByTenantIdAndIdInAndDeletedAtIsNull(UUID tenantId,
                                                                            java.util.Collection<UUID> ids);

    @Query(value = """
            select s from StudentJpaEntity s
            where s.tenantId = :tenantId
              and s.deletedAt is null
              and (:status = '' or s.status = :status)
              and (:search = ''
                   or lower(s.name) like lower(concat('%', :search, '%'))
                   or lower(coalesce(s.socialName, '')) like lower(concat('%', :search, '%'))
                   or lower(s.registrationNumber) like lower(concat('%', :search, '%')))
              and (:document = '' or exists (
                   select 1 from StudentDocumentJpaEntity d
                   where d.studentId = s.id and d.tenantId = s.tenantId
                     and d.deletedAt is null
                     and d.normalizedNumber like concat('%', :document, '%')))
              and (:branchId is null or exists (
                   select 1 from StudentBranchAssignmentJpaEntity a
                   where a.studentId = s.id and a.tenantId = s.tenantId
                     and a.endedAt is null and a.branchId = :branchId))
            """,
            countQuery = """
            select count(s) from StudentJpaEntity s
            where s.tenantId = :tenantId
              and s.deletedAt is null
              and (:status = '' or s.status = :status)
              and (:search = ''
                   or lower(s.name) like lower(concat('%', :search, '%'))
                   or lower(coalesce(s.socialName, '')) like lower(concat('%', :search, '%'))
                   or lower(s.registrationNumber) like lower(concat('%', :search, '%')))
              and (:document = '' or exists (
                   select 1 from StudentDocumentJpaEntity d
                   where d.studentId = s.id and d.tenantId = s.tenantId
                     and d.deletedAt is null
                     and d.normalizedNumber like concat('%', :document, '%')))
              and (:branchId is null or exists (
                   select 1 from StudentBranchAssignmentJpaEntity a
                   where a.studentId = s.id and a.tenantId = s.tenantId
                     and a.endedAt is null and a.branchId = :branchId))
            """)
    Page<StudentJpaEntity> search(@Param("tenantId") UUID tenantId,
                                  @Param("status") String status,
                                  @Param("search") String search,
                                  @Param("document") String document,
                                  @Param("branchId") UUID branchId,
                                  Pageable pageable);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
            update StudentJpaEntity s set s.deletedAt = :at
            where s.id = :studentId and s.tenantId = :tenantId and s.deletedAt is null
            """)
    int softDelete(@Param("tenantId") UUID tenantId, @Param("studentId") UUID studentId,
                   @Param("at") Instant at);
}
