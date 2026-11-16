package com.gymsaas.student.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface StudentContactJpaRepository extends JpaRepository<StudentContactJpaEntity, UUID> {

    List<StudentContactJpaEntity> findByTenantIdAndStudentIdAndDeletedAtIsNullOrderByCreatedAtAsc(
            UUID tenantId, UUID studentId);

    Optional<StudentContactJpaEntity> findByIdAndTenantIdAndStudentIdAndDeletedAtIsNull(
            UUID id, UUID tenantId, UUID studentId);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
            update StudentContactJpaEntity c set c.isPrimary = false
            where c.tenantId = :tenantId and c.studentId = :studentId
              and c.deletedAt is null and c.isPrimary = true
              and (:excludeId is null or c.id <> :excludeId)
            """)
    int clearPrimary(@Param("tenantId") UUID tenantId, @Param("studentId") UUID studentId,
                     @Param("excludeId") UUID excludeId);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
            update StudentContactJpaEntity c set c.deletedAt = :at
            where c.id = :contactId and c.tenantId = :tenantId and c.studentId = :studentId
              and c.deletedAt is null
            """)
    int softDelete(@Param("tenantId") UUID tenantId, @Param("studentId") UUID studentId,
                   @Param("contactId") UUID contactId, @Param("at") Instant at);
}
