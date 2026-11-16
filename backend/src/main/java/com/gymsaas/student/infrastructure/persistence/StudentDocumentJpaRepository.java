package com.gymsaas.student.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface StudentDocumentJpaRepository extends JpaRepository<StudentDocumentJpaEntity, UUID> {

    List<StudentDocumentJpaEntity> findByTenantIdAndStudentIdAndDeletedAtIsNullOrderByCreatedAtAsc(
            UUID tenantId, UUID studentId);

    Optional<StudentDocumentJpaEntity> findByIdAndTenantIdAndStudentIdAndDeletedAtIsNull(
            UUID id, UUID tenantId, UUID studentId);

    List<StudentDocumentJpaEntity> findByTenantIdAndStudentIdInAndDocumentTypeAndDeletedAtIsNull(
            UUID tenantId, Collection<UUID> studentIds, String documentType);

    boolean existsByTenantIdAndDocumentTypeAndNormalizedNumberAndDeletedAtIsNull(
            UUID tenantId, String documentType, String normalizedNumber);

    boolean existsByTenantIdAndDocumentTypeAndNormalizedNumberAndDeletedAtIsNullAndIdNot(
            UUID tenantId, String documentType, String normalizedNumber, UUID excludeId);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
            update StudentDocumentJpaEntity d set d.isPrimary = false
            where d.tenantId = :tenantId and d.studentId = :studentId
              and d.documentType = :type and d.deletedAt is null and d.isPrimary = true
              and (:excludeId is null or d.id <> :excludeId)
            """)
    int clearPrimary(@Param("tenantId") UUID tenantId, @Param("studentId") UUID studentId,
                     @Param("type") String type, @Param("excludeId") UUID excludeId);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
            update StudentDocumentJpaEntity d set d.deletedAt = :at
            where d.id = :documentId and d.tenantId = :tenantId and d.studentId = :studentId
              and d.deletedAt is null
            """)
    int softDelete(@Param("tenantId") UUID tenantId, @Param("studentId") UUID studentId,
                   @Param("documentId") UUID documentId, @Param("at") Instant at);
}
